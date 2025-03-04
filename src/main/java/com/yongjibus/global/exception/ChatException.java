package com.yongjibus.global.exception;

import lombok.Getter;

@Getter
public class ChatException extends RuntimeException {
    private final ErrorCode errorCode;

    public ChatException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
