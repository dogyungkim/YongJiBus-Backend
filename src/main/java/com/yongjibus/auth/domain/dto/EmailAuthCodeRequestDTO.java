package com.yongjibus.auth.domain.dto;

import com.yongjibus.global.email.MjuEmail;

import jakarta.validation.constraints.NotBlank;

public record EmailAuthCodeRequestDTO(
    @MjuEmail
    @NotBlank(message = "이메일을 입력해주세요.")
    String email
) {}
