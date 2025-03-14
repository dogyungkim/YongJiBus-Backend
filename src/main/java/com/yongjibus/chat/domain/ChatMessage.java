package com.yongjibus.chat.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class ChatMessage {
    
    public enum MessageType {
        MESSAGE,       // 일반 채팅 메시지
        ENTER,      // 채팅방 입장 메시지
        LEAVE       // 채팅방 퇴장 메시지
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String sender;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    private LocalDateTime createdAt;

    @Builder
    public ChatMessage(MessageType messageType, String content, String sender, Long roomId, LocalDateTime createdAt) {
        this.messageType = messageType;
        this.content = content;
        this.sender = sender;
        this.roomId = roomId;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
