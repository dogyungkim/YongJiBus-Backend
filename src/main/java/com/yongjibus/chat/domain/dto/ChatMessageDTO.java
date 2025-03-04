package com.yongjibus.chat.domain.dto;

import java.time.LocalDateTime;

import com.yongjibus.chat.domain.ChatMessage;

public record ChatMessageDTO(String content, String sender, Long roomId, LocalDateTime createdAt) {
    public static ChatMessageDTO from(ChatMessage message) {
        return new ChatMessageDTO(message.getContent(), message.getSender(), message.getRoomId(), message.getCreatedAt());
    }
}