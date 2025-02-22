package com.yongjibus.global.exception;

import lombok.Getter;

@Getter
public class DateInfoNotFoundException extends RuntimeException {
    private final ErrorCode errorCode;

    public DateInfoNotFoundException(ErrorCode errorCode, String date) {
        super(date + "의 " + errorCode.getMessage());
        this.errorCode = errorCode;
    }
} 