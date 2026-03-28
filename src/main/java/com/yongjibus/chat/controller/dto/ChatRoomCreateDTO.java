package com.yongjibus.chat.controller.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ChatRoomCreateDTO {
    @NotBlank(message = "채팅방 이름을 입력해주세요.")
    private String name;

    @NotNull(message = "출발 시간을 입력해주세요.")
    private LocalTime departureTime;
} 
