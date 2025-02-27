package com.yongjibus.chatting.domain.dto;

import java.time.LocalTime;

import lombok.Getter;

@Getter
public class ChatRoomCreateDTO {
    private String name;
    private LocalTime departureTime;
} 