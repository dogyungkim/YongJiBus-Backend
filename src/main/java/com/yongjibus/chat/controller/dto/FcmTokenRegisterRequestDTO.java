package com.yongjibus.chat.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenRegisterRequestDTO(
    @NotBlank(message = "FCM 토큰을 입력해주세요.")
    String token
) {
  
} 
