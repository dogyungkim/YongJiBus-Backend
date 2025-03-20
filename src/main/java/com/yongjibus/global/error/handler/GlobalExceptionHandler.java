package com.yongjibus.global.error.handler;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.yongjibus.global.common.response.YongJiResponse;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.AuthException;
import com.yongjibus.global.error.exception.ChatException;
import com.yongjibus.global.error.exception.DateInfoNotFoundException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DateInfoNotFoundException.class)
    public ResponseEntity<YongJiResponse<String>> handleNotFoundException(DateInfoNotFoundException e) {
        log.error("DateInfoNotFoundException: {}", e.getMessage());
        return YongJiResponse.error(e.getErrorCode().getStatus().value(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<YongJiResponse<String>> handleValidationExceptions(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult()
                .getAllErrors()
                .get(0)
                .getDefaultMessage();
        log.error("MethodArgumentNotValidException: {}", errorMessage);
        return YongJiResponse.error(ErrorCode.INVALID_DATE_FORMAT.getStatus().value(), errorMessage);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<YongJiResponse<String>> handleAuthException(AuthException e) {
        log.error("AuthException: {}", e.getMessage());
        return YongJiResponse.error(e.getErrorCode().getStatus().value(), e.getMessage());
    }

    @ExceptionHandler(ChatException.class)
    public ResponseEntity<YongJiResponse<String>> handleChatException(ChatException e) {
        log.error("ChatException: {}", e.getMessage());
        return YongJiResponse.error(e.getErrorCode().getStatus().value(), e.getMessage());
    }
}

