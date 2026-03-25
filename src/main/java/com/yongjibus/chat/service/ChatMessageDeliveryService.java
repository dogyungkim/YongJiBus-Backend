package com.yongjibus.chat.service;

import com.yongjibus.chat.domain.ChatMessage;
import com.yongjibus.global.infra.websocket.WebsocketSessionManager;
import com.yongjibus.member.domain.Member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageDeliveryService {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebsocketSessionManager websocketSessionManager;
    private final FCMNotificationService fcmNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void deliver(ChatMessageDeliveryEvent event) {
        ChatMessage message = event.message();

        for (Member member : event.recipients()) {
            if (!websocketSessionManager.isSessionExists(member.getEmail())
                && message.getMessageType() == ChatMessage.MessageType.MESSAGE) {
                try {
                    fcmNotificationService.sendChatNotification(
                        message,
                        member,
                        event.roomId(),
                        event.roomName()
                    );
                } catch (Exception e) {
                    log.warn("FCM 알림 전송 실패로 푸시를 건너뜁니다. roomId={}, memberId={}", event.roomId(), member.getId(), e);
                }
            }
        }

        messagingTemplate.convertAndSend("/sub/chat/room/" + event.roomId(), message);
    }
}
