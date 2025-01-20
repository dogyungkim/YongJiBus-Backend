package com.yongjibus.exception;

public class DateInfoNotFoundException extends RuntimeException {
    public DateInfoNotFoundException(String message) {
        super("DateInfo not found : " + message);
    }
} 