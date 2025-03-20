package com.yongjibus.auth.controller.dto;

import com.yongjibus.global.infra.email.MjuEmail;

import jakarta.validation.constraints.NotBlank;

public record EmailAuthCodeRequestDTO(
    @MjuEmail
    @NotBlank(message = "이메일을 입력해주세요.")
    String email
) {} 