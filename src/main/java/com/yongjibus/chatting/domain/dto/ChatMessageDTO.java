package com.yongjibus.chatting.domain.dto;

import java.time.LocalDateTime;

public record ChatMessageDTO(String content, String sender, Long roomId, LocalDateTime createdAt) {
}