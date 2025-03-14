package com.yongjibus.chat.domain.dto;

import java.time.LocalDateTime;

import com.yongjibus.chat.domain.ChatMessage;
import com.yongjibus.chat.domain.ChatMessage.MessageType;

public record ChatMessageResponseDTO(Long id, MessageType messageType, String content, String sender, Long roomId, LocalDateTime createdAt) {
    public static ChatMessageResponseDTO from(ChatMessage message) {
        return new ChatMessageResponseDTO(message.getId(), message.getMessageType(), message.getContent(), message.getSender(), message.getRoomId(), message.getCreatedAt());
    }
}
