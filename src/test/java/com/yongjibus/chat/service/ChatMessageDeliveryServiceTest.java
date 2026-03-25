package com.yongjibus.chat.service;

import com.yongjibus.chat.domain.ChatMessage;
import com.yongjibus.global.infra.websocket.WebsocketSessionManager;
import com.yongjibus.member.domain.Member;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageDeliveryServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private WebsocketSessionManager websocketSessionManager;

    @Mock
    private FCMNotificationService fcmNotificationService;

    @InjectMocks
    private ChatMessageDeliveryService chatMessageDeliveryService;

    private Member testMember;
    private ChatMessage message;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .email("test@example.com")
                .name("이영진")
                .username("테스트유저")
                .build();

        message = ChatMessage.builder()
                .messageType(ChatMessage.MessageType.MESSAGE)
                .content("테스트 메시지")
                .sender("테스트유저")
                .roomId(1L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("오프라인 사용자에게 일반 메시지를 배달할 때는 푸시와 브로드캐스트를 모두 수행한다")
    void deliver_WhenRecipientOffline_ShouldSendPushAndBroadcast() {
        // given
        ChatMessageDeliveryEvent event = new ChatMessageDeliveryEvent(message, 1L, "테스트 채팅방", List.of(testMember));
        when(websocketSessionManager.isSessionExists(testMember.getEmail())).thenReturn(false);

        // when
        chatMessageDeliveryService.deliver(event);

        // then
        verify(fcmNotificationService).sendChatNotification(message, testMember, 1L, "테스트 채팅방");
        verify(messagingTemplate).convertAndSend("/sub/chat/room/1", message);
    }

    @Test
    @DisplayName("온라인 사용자에게는 푸시를 보내지 않고 브로드캐스트만 수행한다")
    void deliver_WhenRecipientOnline_ShouldBroadcastOnly() {
        // given
        ChatMessageDeliveryEvent event = new ChatMessageDeliveryEvent(message, 1L, "테스트 채팅방", List.of(testMember));
        when(websocketSessionManager.isSessionExists(testMember.getEmail())).thenReturn(true);

        // when
        chatMessageDeliveryService.deliver(event);

        // then
        verifyNoInteractions(fcmNotificationService);
        verify(messagingTemplate).convertAndSend("/sub/chat/room/1", message);
    }

    @Test
    @DisplayName("시스템 메시지는 오프라인 사용자에게도 푸시를 보내지 않는다")
    void deliver_WhenSystemMessage_ShouldSkipPush() {
        // given
        ChatMessage systemMessage = ChatMessage.builder()
                .messageType(ChatMessage.MessageType.SYSTEM)
                .content("경고")
                .sender("시스템")
                .roomId(1L)
                .createdAt(LocalDateTime.now())
                .build();
        ChatMessageDeliveryEvent event = new ChatMessageDeliveryEvent(systemMessage, 1L, "테스트 채팅방", List.of(testMember));
        when(websocketSessionManager.isSessionExists(testMember.getEmail())).thenReturn(false);

        // when
        chatMessageDeliveryService.deliver(event);

        // then
        verify(fcmNotificationService, never()).sendChatNotification(any(), any(), anyLong(), anyString());
        verify(messagingTemplate).convertAndSend("/sub/chat/room/1", systemMessage);
    }

    @Test
    @DisplayName("푸시 전송이 실패해도 브로드캐스트는 계속된다")
    void deliver_WhenPushFails_ShouldStillBroadcast() {
        // given
        ChatMessageDeliveryEvent event = new ChatMessageDeliveryEvent(message, 1L, "테스트 채팅방", List.of(testMember));
        when(websocketSessionManager.isSessionExists(testMember.getEmail())).thenReturn(false);
        doThrow(new RuntimeException("fcm failed"))
            .when(fcmNotificationService)
            .sendChatNotification(any(), any(), anyLong(), anyString());

        // when & then
        assertThatCode(() -> chatMessageDeliveryService.deliver(event))
            .doesNotThrowAnyException();
        verify(messagingTemplate).convertAndSend("/sub/chat/room/1", message);
    }
}
