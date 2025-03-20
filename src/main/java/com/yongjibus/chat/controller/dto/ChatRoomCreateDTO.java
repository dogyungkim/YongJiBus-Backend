package com.yongjibus.chat.controller.dto;

import java.time.LocalTime;

import lombok.Getter;

@Getter
public class ChatRoomCreateDTO {
    private String name;
    private LocalTime departureTime;
} 