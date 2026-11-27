package com.yongjibus.member.controller.dto;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequestDTO(
        @NotBlank
        @Length(min = 8, max = 20, message = "현재 비밀번호는 8자 이상 20자 이하여야 합니다.")
        String currentPassword,

        @NotBlank
        @Length(min = 8, max = 20, message = "새 비밀번호는 8자 이상 20자 이하여야 합니다.")
        String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordRequestDTO[redacted]";
    }
}
