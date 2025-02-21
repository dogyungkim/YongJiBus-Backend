package com.yongjibus.auth.domain.dto;

import org.hibernate.validator.constraints.Length;

import com.yongjibus.global.email.MjuEmail;

import jakarta.validation.constraints.NotBlank;

public record EmailVerifyRequestDTO(
    @MjuEmail
    @NotBlank
    String email,

    @NotBlank
    @Length(min = 6, max = 6, message = "인증 코드는 6자리여야 합니다.")
    String authCode
) {}
