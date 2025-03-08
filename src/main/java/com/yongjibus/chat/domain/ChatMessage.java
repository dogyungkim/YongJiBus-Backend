package com.yongjibus.chat.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String sender;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    private LocalDateTime createdAt;

    @Builder
    public ChatMessage(String content, String sender, Long roomId, LocalDateTime createdAt) {
        this.content = content;
        this.sender = sender;
        this.roomId = roomId;
        this.createdAt = createdAt;
    }
}
