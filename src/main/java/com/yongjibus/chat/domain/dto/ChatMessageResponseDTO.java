package com.yongjibus.chat.domain.dto;

import java.time.LocalDateTime;

import com.yongjibus.chat.domain.ChatMessage;

public record ChatMessageResponseDTO(Long id, String content, String sender, Long roomId, LocalDateTime createdAt) {
    public static ChatMessageResponseDTO from(ChatMessage message) {
        return new ChatMessageResponseDTO(message.getId(), message.getContent(), message.getSender(), message.getRoomId(), message.getCreatedAt());
    }
}
