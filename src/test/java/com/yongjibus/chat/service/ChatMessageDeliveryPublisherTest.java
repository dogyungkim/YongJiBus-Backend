package com.yongjibus.chat.service;

import com.yongjibus.chat.domain.ChatMessage;
import com.yongjibus.chat.domain.ChatRoom;
import com.yongjibus.chat.repository.ChatRoomRepository;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.ChatException;
import com.yongjibus.member.domain.Member;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageDeliveryPublisherTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private ChatMessageDeliveryPublisher chatMessageDeliveryPublisher;

    private Member testMember;
    private ChatRoom testChatRoom;
    private ChatMessage testChatMessage;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .email("test@example.com")
                .name("이영진")
                .username("테스트유저")
                .build();

        testChatRoom = ChatRoom.builder()
                .name("테스트 채팅방")
                .departureTime(LocalTime.of(14, 0))
                .build();
        ReflectionTestUtils.setField(testChatRoom, "id", 1L);
        testChatRoom.addMember(testMember);

        testChatMessage = ChatMessage.builder()
                .messageType(ChatMessage.MessageType.MESSAGE)
                .content("테스트 메시지")
                .sender("테스트유저")
                .roomId(1L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("roomId로 배달을 요청하면 채팅방 스냅샷 이벤트를 발행한다")
    void publishAfterCommit_WithRoomId_ShouldPublishSnapshotEvent() {
        // given
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));

        // when
        chatMessageDeliveryPublisher.publishAfterCommitByRoomId(testChatMessage, 1L);

        // then
        ArgumentCaptor<ChatMessageDeliveryEvent> captor = ArgumentCaptor.forClass(ChatMessageDeliveryEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        ChatMessageDeliveryEvent event = captor.getValue();
        assertThat(event.message()).isSameAs(testChatMessage);
        assertThat(event.roomId()).isEqualTo(1L);
        assertThat(event.roomName()).isEqualTo("테스트 채팅방");
        assertThat(event.recipients()).containsExactly(testMember);
    }

    @Test
    @DisplayName("채팅방 엔티티가 있으면 추가 조회 없이 배달 이벤트를 발행한다")
    void publishAfterCommit_WithChatRoom_ShouldPublishWithoutLookup() {
        // when
        chatMessageDeliveryPublisher.publishAfterCommitWithRoom(testChatMessage, testChatRoom);

        // then
        verifyNoInteractions(chatRoomRepository);
        verify(applicationEventPublisher).publishEvent(eq(new ChatMessageDeliveryEvent(
            testChatMessage,
            1L,
            "테스트 채팅방",
            testChatRoom.getMembers()
        )));
    }

    @Test
    @DisplayName("존재하지 않는 채팅방이면 배달 이벤트를 발행하지 않고 예외를 던진다")
    void publishAfterCommit_WhenRoomNotFound_ShouldThrow() {
        // given
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatMessageDeliveryPublisher.publishAfterCommitByRoomId(testChatMessage, 1L))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ROOM_NOT_FOUND);
        verifyNoInteractions(applicationEventPublisher);
    }
}
