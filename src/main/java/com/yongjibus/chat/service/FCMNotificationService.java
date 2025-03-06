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

    public void sendChatNotification(ChatMessage chatMessage, ChatRoom chatRoom) {
        try {
            // 메시지 발신자 정보 추출
            String senderName = chatMessage.getSender();
            // 알림을 받을 사용자 목록 조회 (오프라인이면서 알림 설정이 활성화된 사용자)
            List<Member> roomMembers = chatRoom.getMembers();
            
            // 각 사용자별 FCM 토큰 수집
            Map<Member, String> memberTokensMap = new HashMap<>();
            for (Member member : roomMembers) {
                if (senderName.equals(member.getUsername())) {
                    continue; // 발신자에게는 알림을 보내지 않음
                }
                
                FCMToken token = fcmTokenService.getActiveTokenByMember(member);
                if (token != null) {
                    memberTokensMap.put(member, token.getToken());
                }
            }
            
            if (memberTokensMap.isEmpty()) {
                return; // 알림을 받을 사용자가 없음
            }

            // 각 사용자별로 알림 전송
            for (Map.Entry<Member, String> entry : memberTokensMap.entrySet()) {
                Member member = entry.getKey();
                String token = entry.getValue();
                
                // 알림 데이터 준비
                String title = chatRoom.getName();
                String body = senderName + ": " + chatMessage.getContent();
                
                Map<String, String> data = new HashMap<>();
                data.put("chatRoomId", chatRoom.getId().toString());
                data.put("messageId", chatMessage.getId().toString());
                data.put("senderName", senderName);
                
                // 멀티캐스트 메시지 생성
                MulticastMessage fcmMessage = MulticastMessage.builder()
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putAllData(data)
                        .addAllTokens(Arrays.asList(token))
                        .build();
                
                // FCM 메시지 발송
                BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(fcmMessage);
                
                // 실패한 토큰 처리
                if (response.getFailureCount() > 0) {
                    List<SendResponse> responses = response.getResponses();
                    for (int i = 0; i < responses.size(); i++) {
                        if (!responses.get(i).isSuccessful()) {
                            if (responses.get(i).getException() != null && 
                                responses.get(i).getException().getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                                fcmTokenService.deactivateToken(token);
                            }
                            log.error("Failed to send message to token: {} - Error: {}", 
                                    token, responses.get(i).getException());
                        }
                    }
                }
                
                log.info("FCM Notification sent to member {}: Success: {}, Failure: {}", 
                        member.getUsername(), response.getSuccessCount(), response.getFailureCount());
            }
            
        } catch (Exception e) {
            log.error("FCM 알림 전송 중 오류 발생", e);
        }
    }
} 