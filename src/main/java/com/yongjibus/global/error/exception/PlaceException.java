package com.yongjibus.global.error.exception;

import com.yongjibus.global.error.code.ErrorCode;

import lombok.Getter;

@Getter
public class PlaceException extends RuntimeException {
    private final ErrorCode errorCode;

    public PlaceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
