package com.yongjibus.chat.domain.dto;

import java.time.LocalDateTime;

import com.yongjibus.chat.domain.ChatMessage;
import com.yongjibus.chat.domain.ChatMessage.MessageType;

public record ChatMessageDTO(MessageType messageType, String content, String sender, Long roomId, LocalDateTime createdAt) {
    public static ChatMessageDTO from(ChatMessage message) {
        return new ChatMessageDTO(message.getMessageType(), message.getContent(), message.getSender(), message.getRoomId(), message.getCreatedAt());
    }
}