package com.yongjibus.chat.service;

import com.yongjibus.chat.domain.ChatMessage;
import com.yongjibus.global.infra.websocket.WebsocketSessionManager;
import com.yongjibus.member.domain.Member;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(ChatMessageDeliveryServiceTransactionalTest.TestConfig.class)
class ChatMessageDeliveryServiceTransactionalTest {

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override
                protected Object doGetTransaction() {
                    return new Object();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) {
                }

                @Override
                protected void doCommit(DefaultTransactionStatus status) {
                }

                @Override
                protected void doRollback(DefaultTransactionStatus status) {
                }
            };
        }

        @Bean
        SimpMessagingTemplate messagingTemplate() {
            return mock(SimpMessagingTemplate.class);
        }

        @Bean
        WebsocketSessionManager websocketSessionManager() {
            return mock(WebsocketSessionManager.class);
        }

        @Bean
        FCMNotificationService fcmNotificationService() {
            return mock(FCMNotificationService.class);
        }

        @Bean
        ChatMessageDeliveryService chatMessageDeliveryService(
            SimpMessagingTemplate messagingTemplate,
            WebsocketSessionManager websocketSessionManager,
            FCMNotificationService fcmNotificationService
        ) {
            return new ChatMessageDeliveryService(messagingTemplate, websocketSessionManager, fcmNotificationService);
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @org.springframework.beans.factory.annotation.Autowired
    private PlatformTransactionManager transactionManager;

    @org.springframework.beans.factory.annotation.Autowired
    private SimpMessagingTemplate messagingTemplate;

    @org.springframework.beans.factory.annotation.Autowired
    private WebsocketSessionManager websocketSessionManager;

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
    @DisplayName("배달 이벤트는 트랜잭션 커밋 전에는 실행되지 않고 커밋 후에만 실행된다")
    void deliver_ShouldRunAfterCommit() {
        // given
        ChatMessageDeliveryEvent event = new ChatMessageDeliveryEvent(message, 1L, "테스트 채팅방", List.of(testMember));
        when(websocketSessionManager.isSessionExists(testMember.getEmail())).thenReturn(true);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        // when
        transactionTemplate.executeWithoutResult(status -> {
            applicationEventPublisher.publishEvent(event);
            verifyNoInteractions(messagingTemplate);
        });

        // then
        verify(messagingTemplate).convertAndSend("/sub/chat/room/1", message);
    }
}
