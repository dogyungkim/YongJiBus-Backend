package com.yongjibus.chat.domain.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;

import com.yongjibus.chat.domain.ChatRoom;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatRoomResponseDTO {
    private Long id;
    private String name;
    private LocalTime departureTime;
    private int userCount;
    private LocalDateTime createdAt;

    public static ChatRoomResponseDTO from(ChatRoom chatRoom) {
        return ChatRoomResponseDTO.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .departureTime(chatRoom.getDepartureTime())
                .userCount(chatRoom.getUserCount())
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }
} 