package com.yongjibus.global.error.exception;

import com.yongjibus.global.error.code.ErrorCode;

import lombok.Getter;

@Getter
public class TimetableException extends RuntimeException {
    private final ErrorCode errorCode;

    public TimetableException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
