package com.yongjibus.chat.repository;

import org.springframework.stereotype.Service;

import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

@Service
public class FCMService {
    public void sendNotification(String title, String body, String token){
        Notification notification = Notification.builder()
        .setTitle(title)
        .setBody(body)
        .build();
        
        Message message = Message.builder()
            .setToken(token)
            .setNotification(notification)
            .build();
    }
}
