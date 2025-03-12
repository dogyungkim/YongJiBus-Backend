package com.yongjibus.chat.service;

import com.google.firebase.messaging.*;
import com.yongjibus.auth.domain.Member;
import com.yongjibus.chat.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FCMNotificationService {

    private final FCMTokenService fcmTokenService;

    public void sendChatNotification(ChatMessage chatMessage, Member member, ChatRoom chatRoom) {
 
            // 메시지 발신자 정보 추출
            String senderName = chatMessage.getSender();

            FCMToken token = fcmTokenService.getActiveTokenByMember(member);

            if (token == null) {
                return;
            }

            // 알림 데이터 준비
            String title = chatRoom.getName();
            String body = senderName + ": " + chatMessage.getContent();
            Map<String, String> data = new HashMap<>();

            data.put("chatRoomId", chatRoom.getId().toString());
            data.put("messageId", chatMessage.getId().toString());
            data.put("senderName", senderName);

            Message message = Message.builder()
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putAllData(data)
                .setToken(token.getToken())
                .build();
        try {
            // 메시지 전송
            String response = FirebaseMessaging.getInstance().send(message);
            
            log.info("FCM Notification sent to member {}: {}", 
                    member.getUsername(), response);
        } catch (Exception e) {
            log.error("FCM 알림 전송 중 오류 발생", e);
        }
    }
} 