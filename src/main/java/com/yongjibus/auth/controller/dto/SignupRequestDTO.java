package com.yongjibus.auth.controller.dto;

import org.hibernate.validator.constraints.Length;

import com.yongjibus.global.infra.email.MjuEmail;
import com.yongjibus.member.domain.Member;

import jakarta.validation.constraints.NotBlank;

public record SignupRequestDTO(
    @MjuEmail
    @NotBlank
    String email,

    @NotBlank
    @Length(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
    String password,

    @NotBlank
    @Length(min = 2, max = 10, message = "이름은 2자 이상 10자 이하여야 합니다.")
    String name,

    @NotBlank
    @Length(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하여야 합니다.")
    String username
) {
    public Member toEntity() {
        return Member.builder()
            .email(email)
            .password(password)
            .name(name)
            .username(username)
            .build();
    }
} 