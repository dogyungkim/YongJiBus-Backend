package com.yongjibus.chat.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FcmTokenRegisterRequestDTO(
    @NotBlank(message = "FCM 토큰을 입력해주세요.")
    @Size(max = 512, message = "FCM 토큰이 너무 깁니다.")
    @Pattern(regexp = "\\S+", message = "FCM 토큰을 입력해주세요.")
    String token
) {
  
} 
