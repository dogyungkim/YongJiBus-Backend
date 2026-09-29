package com.yongjibus.chat.service;

import com.google.firebase.messaging.*;
import com.yongjibus.chat.domain.*;
import com.yongjibus.member.domain.Member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FCMNotificationService {

    private final FCMTokenService fcmTokenService;

    public void sendChatNotification(ChatMessage chatMessage, Member member, Long chatRoomId, String chatRoomName) {
        if (Boolean.TRUE.equals(member.getIsDeleted())) {
            return;
        }

        List<FCMToken> tokens = fcmTokenService.findActiveTokensByMember(member);
        if (tokens.isEmpty()) {
            log.debug("활성 FCM 토큰이 없어 알림을 건너뜁니다. memberId={}", member.getId());
            return;
        }

        String senderName = chatMessage.getSender();
        String title = chatRoomName;
        String body = senderName + ": " + chatMessage.getContent();
        Map<String, String> data = new HashMap<>();
        data.put("type", "chat");
        data.put("chatRoomId", chatRoomId.toString());
        data.put("messageId", chatMessage.getId().toString());
        data.put("senderName", senderName);

        for (FCMToken token : tokens) {
            Message message = Message.builder()
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .putAllData(data)
                    .setToken(token.getToken())
                    .build();
            try {
                String response = FirebaseMessaging.getInstance().send(message);
                log.info("FCM Notification sent to member {}: {}", member.getUsername(), response);
            } catch (Exception e) {
                log.error("FCM 알림 전송 중 오류 발생", e);
            }
        }
    }
} 
