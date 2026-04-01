package com.yongjibus.chat.service;

import com.yongjibus.chat.domain.ChatMessage;
import com.yongjibus.chat.domain.ChatRoom;
import com.yongjibus.chat.domain.ChatRoomMember;
import com.yongjibus.chat.repository.ChatRepository;
import com.yongjibus.chat.repository.ChatRoomMemberRepository;
import com.yongjibus.chat.repository.ChatRoomRepository;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.ChatException;
import com.yongjibus.member.domain.Member;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatMessageDeliveryPublisher chatMessageDeliveryPublisher;

    @InjectMocks
    private ChatService chatService;

    private Member testMember;
    private Member testMember2;
    private ChatRoom testChatRoom;
    private ChatMessage testChatMessage;
    private ChatRoomMember testChatRoomMember;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .email("test@example.com")
                .name("이영진")
                .username("테스트유저")
                .build();

        testMember2 = Member.builder()
                .id(2L)
                .email("test2@example.com")
                .name("이영진2")
                .username("테스트유저2")
                .build();

        testChatRoom = ChatRoom.builder()
                .name("테스트 채팅방")
                .departureTime(LocalTime.of(14, 0))
                .build();
        ReflectionTestUtils.setField(testChatRoom, "id", 1L);

        testChatMessage = ChatMessage.builder()
                .messageType(ChatMessage.MessageType.MESSAGE)
                .content("테스트 메시지")
                .sender("테스트유저")
                .roomId(1L)
                .createdAt(LocalDateTime.now())
                .build();

        testChatRoomMember = ChatRoomMember.builder()
                .member(testMember)
                .chatRoom(testChatRoom)
                .joinedAt(LocalDateTime.now())
                .active(true)
                .build();
    }

    @Test
    @DisplayName("채팅방 생성 시 저장 후 초기 시스템 메시지 배달을 위임한다")
    void createChatRoomTest() {
        // given
        String roomName = "새로운 채팅방";
        LocalTime departureTime = LocalTime.of(15, 30);

        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> {
            ChatRoom room = invocation.getArgument(0);
            if (room.getId() == null) {
                ReflectionTestUtils.setField(room, "id", 1L);
            }
            return room;
        });
        when(chatRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ChatRoom result = chatService.createChatRoom(roomName, departureTime, testMember);

        // then
        assertThat(result).isNotNull();
        verify(chatRoomRepository, times(2)).save(any(ChatRoom.class));
        verify(chatRepository).save(argThat(message ->
            message.getMessageType() == ChatMessage.MessageType.SYSTEM
                && Long.valueOf(1L).equals(message.getRoomId())
        ));
        verify(chatMessageDeliveryPublisher).publishAfterCommitWithRoom(
            argThat(message -> message.getMessageType() == ChatMessage.MessageType.SYSTEM),
            any(ChatRoom.class)
        );
    }

    @Test
    @DisplayName("채팅방 참여 시 저장 후 입장 메시지 배달을 위임한다")
    void joinChatRoomTest() {
        // given
        when(chatRoomRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(testChatRoom));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(testChatRoom);
        when(chatRoomMemberRepository.findByMemberAndChatRoomAndActiveTrue(testMember, testChatRoom))
            .thenReturn(Optional.empty());

        // when
        ChatRoom result = chatService.joinChatRoom(1L, testMember);

        // then
        assertThat(result).isNotNull();
        verify(chatRepository).save(argThat(message ->
            message.getMessageType() == ChatMessage.MessageType.ENTER
                && message.getContent().contains(testMember.getUsername())
        ));
        verify(chatMessageDeliveryPublisher).publishAfterCommitByRoomId(
            argThat(message -> message.getMessageType() == ChatMessage.MessageType.ENTER),
            eq(1L)
        );
    }

    @Test
    @DisplayName("이미 참여 중인 사용자는 정원이 가득 차도 기존 채팅방을 그대로 반환한다")
    void joinChatRoom_WhenMemberAlreadyJoinedAndRoomIsFull_ShouldReturnExistingRoom() {
        // given
        ChatRoom fullChatRoom = ChatRoom.builder()
                .name("가득 찬 채팅방")
                .departureTime(LocalTime.of(14, 0))
                .build();
        ReflectionTestUtils.setField(fullChatRoom, "id", 1L);
        fullChatRoom.addMember(testMember);
        fullChatRoom.addMember(Member.builder().id(2L).username("user2").build());
        fullChatRoom.addMember(Member.builder().id(3L).username("user3").build());
        fullChatRoom.addMember(Member.builder().id(4L).username("user4").build());
        fullChatRoom.addMember(Member.builder().id(5L).username("user5").build());

        when(chatRoomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fullChatRoom));
        when(chatRoomMemberRepository.findByMemberAndChatRoomAndActiveTrue(testMember, fullChatRoom))
            .thenReturn(Optional.of(testChatRoomMember));

        // when
        ChatRoom result = chatService.joinChatRoom(1L, testMember);

        // then
        assertThat(result).isSameAs(fullChatRoom);
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
        verify(chatRepository, never()).save(any(ChatMessage.class));
        verifyNoInteractions(chatMessageDeliveryPublisher);
    }

    @Test
    @DisplayName("새 사용자는 정원이 가득 찬 채팅방에 참여할 수 없다")
    void joinChatRoom_WhenRoomIsFull_ShouldThrow() {
        // given
        ChatRoom fullChatRoom = ChatRoom.builder()
                .name("가득 찬 채팅방")
                .departureTime(LocalTime.of(14, 0))
                .build();
        ReflectionTestUtils.setField(fullChatRoom, "id", 1L);
        fullChatRoom.addMember(Member.builder().id(10L).username("user10").build());
        fullChatRoom.addMember(Member.builder().id(11L).username("user11").build());
        fullChatRoom.addMember(Member.builder().id(12L).username("user12").build());
        fullChatRoom.addMember(Member.builder().id(13L).username("user13").build());
        fullChatRoom.addMember(Member.builder().id(14L).username("user14").build());

        when(chatRoomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fullChatRoom));
        when(chatRoomMemberRepository.findByMemberAndChatRoomAndActiveTrue(testMember2, fullChatRoom))
            .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatService.joinChatRoom(1L, testMember2))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ROOM_FULL);
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
        verify(chatRepository, never()).save(any(ChatMessage.class));
        verifyNoInteractions(chatMessageDeliveryPublisher);
    }

    @Test
    @DisplayName("마지막 참여자가 퇴장하면 채팅방을 삭제하고 퇴장 메시지는 남기지 않는다")
    void leaveChatRoom_WhenLastMemberLeaves_ShouldDeleteRoomWithoutLeaveMessage() {
        // given
        testChatRoom.addMember(testMember);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
        when(chatRoomMemberRepository.findByMemberAndChatRoomAndActiveTrue(testMember, testChatRoom))
            .thenReturn(Optional.of(testChatRoomMember));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        chatService.leaveChatRoom(1L, testMember);

        // then
        verify(chatRoomRepository).delete(testChatRoom);
        verify(chatRepository, never()).save(any(ChatMessage.class));
        verifyNoInteractions(chatMessageDeliveryPublisher);
    }

    @Test
    @DisplayName("채팅방에 다른 참여자가 남아 있으면 퇴장 메시지를 저장하고 배달을 위임한다")
    void leaveChatRoom_WhenRoomStillHasMembers_ShouldSaveLeaveMessage() {
        // given
        testChatRoom.addMember(testMember);
        testChatRoom.addMember(testMember2);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
        when(chatRoomMemberRepository.findByMemberAndChatRoomAndActiveTrue(testMember, testChatRoom))
            .thenReturn(Optional.of(testChatRoomMember));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        chatService.leaveChatRoom(1L, testMember);

        // then
        verify(chatRepository).save(argThat(message ->
            message.getMessageType() == ChatMessage.MessageType.LEAVE
                && message.getContent().contains(testMember.getUsername())
                && Long.valueOf(1L).equals(message.getRoomId())
        ));
        verify(chatMessageDeliveryPublisher).publishAfterCommitByRoomId(
            argThat(message -> message.getMessageType() == ChatMessage.MessageType.LEAVE),
            eq(1L)
        );
        verify(chatRoomRepository, never()).delete(any(ChatRoom.class));
    }

    @Test
    @DisplayName("활성 참여자가 아니면 채팅방을 나갈 수 없다")
    void leaveChatRoom_WhenMemberIsNotActive_ShouldThrowForbidden() {
        // given
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
        when(chatRoomMemberRepository.findByMemberAndChatRoomAndActiveTrue(testMember, testChatRoom))
            .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatService.leaveChatRoom(1L, testMember))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ROOM_FORBIDDEN);
        verify(chatRoomRepository, never()).delete(any(ChatRoom.class));
        verify(chatRepository, never()).save(any(ChatMessage.class));
        verifyNoInteractions(chatMessageDeliveryPublisher);
    }

    @Test
    @DisplayName("채팅방 조회 테스트")
    void getChatRoomTest() {
        // given
        when(chatRoomRepository.findById(anyLong())).thenReturn(Optional.of(testChatRoom));

        // when
        ChatRoom result = chatService.getChatRoom(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("테스트 채팅방");
    }

    @Test
    @DisplayName("채팅방 조회 실패 테스트")
    void getChatRoomNotFoundTest() {
        // given
        when(chatRoomRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThrows(ChatException.class, () -> chatService.getChatRoom(1L));
    }

    @Test
    @DisplayName("메시지 저장 후 배달 위임이 실행된다")
    void processAndSendMessageTest() {
        // given
        when(chatRepository.save(any(ChatMessage.class))).thenReturn(testChatMessage);

        // when
        chatService.processAndSendMessage(testChatMessage);

        // then
        InOrder inOrder = inOrder(chatRepository, chatMessageDeliveryPublisher);
        inOrder.verify(chatRepository).save(testChatMessage);
        inOrder.verify(chatMessageDeliveryPublisher).publishAfterCommitByRoomId(testChatMessage, 1L);
    }

    @Test
    @DisplayName("메시지 저장 실패 시 배달은 시도하지 않는다")
    void processAndSendMessage_WhenSaveFails_ShouldNotPublish() {
        // given
        when(chatRepository.save(any(ChatMessage.class))).thenThrow(new RuntimeException("db error"));

        // when & then
        assertThatThrownBy(() -> chatService.processAndSendMessage(testChatMessage))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_MESSAGE_SAVE_ERROR);
        verifyNoInteractions(chatMessageDeliveryPublisher);
    }

    @Test
    @DisplayName("메시지 전송 시 sender는 서버가 인증된 사용자명으로 채운다")
    void sendMessage_ShouldUseAuthenticatedSender() {
        // given
        testChatRoom.addMember(testMember);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
        when(chatRoomMemberRepository.findByMemberAndChatRoomAndActiveTrue(testMember, testChatRoom))
            .thenReturn(Optional.of(testChatRoomMember));
        when(chatRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        chatService.sendMessage(testMember, 1L, "서버가 보낸 메시지");

        // then
        verify(chatRepository).save(argThat(message ->
            message.getMessageType() == ChatMessage.MessageType.MESSAGE
                && "서버가 보낸 메시지".equals(message.getContent())
                && testMember.getUsername().equals(message.getSender())
                && Long.valueOf(1L).equals(message.getRoomId())
        ));
        verify(chatMessageDeliveryPublisher).publishAfterCommitByRoomId(
            argThat(message ->
                message.getMessageType() == ChatMessage.MessageType.MESSAGE
                    && testMember.getUsername().equals(message.getSender())
            ),
            eq(1L)
        );
    }

    @Test
    @DisplayName("채팅방 참여자가 아니면 메시지를 보낼 수 없다")
    void sendMessage_WhenSenderIsNotRoomMember_ShouldThrowForbidden() {
        // given
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
        when(chatRoomMemberRepository.findByMemberAndChatRoomAndActiveTrue(testMember, testChatRoom))
            .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatService.sendMessage(testMember, 1L, "실패"))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ROOM_FORBIDDEN);
    }

    @Test
    @DisplayName("채팅 메시지 조회 테스트")
    void getChatMessagesTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<ChatMessage> messages = Arrays.asList(testChatMessage);
        Slice<ChatMessage> slice = new SliceImpl<>(messages, pageable, false);

        when(chatRoomRepository.findById(anyLong())).thenReturn(Optional.of(testChatRoom));
        when(chatRoomMemberRepository.findByMemberAndChatRoomAndActiveTrue(any(), any()))
            .thenReturn(Optional.of(testChatRoomMember));
        when(chatRoomMemberRepository.findJoinedAtByMemberAndChatRoom(any(), any()))
            .thenReturn(Optional.of(LocalDateTime.now().minusHours(1)));
        when(chatRepository.findMessagesAfterJoinTime(
            anyLong(), any(LocalDateTime.class), any(Pageable.class))).thenReturn(slice);

        // when
        Slice<ChatMessage> result = chatService.getChatMessages(1L, testMember, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent().size()).isEqualTo(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("테스트 메시지");
    }
}
