package com.yongjibus.auth.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UsernameCheckResponseDTO {
    private boolean exists;
} 