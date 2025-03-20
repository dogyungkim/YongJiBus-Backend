package com.yongjibus.global.error.exception;

import com.yongjibus.global.error.code.ErrorCode;

import lombok.Getter;

@Getter
public class DateInfoNotFoundException extends RuntimeException {
    private final ErrorCode errorCode;

    public DateInfoNotFoundException(ErrorCode errorCode, String date) {
        super(date + "의 " + errorCode.getMessage());
        this.errorCode = errorCode;
    }
} 