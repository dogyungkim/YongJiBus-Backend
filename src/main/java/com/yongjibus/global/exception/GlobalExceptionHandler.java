package com.yongjibus.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.yongjibus.global.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DateInfoNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleNotFoundException(DateInfoNotFoundException e) {
        log.error("DateInfoNotFoundException: {}", e.getMessage());
        return ApiResponse.error(e.getErrorCode().getStatus().value(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidationExceptions(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult()
                .getAllErrors()
                .get(0)
                .getDefaultMessage();
        log.error("MethodArgumentNotValidException: {}", errorMessage);
        return ApiResponse.error(ErrorCode.INVALID_DATE_FORMAT.getStatus().value(), errorMessage);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<String>> handleAuthException(AuthException e) {
        log.error("AuthException: {}", e.getMessage());
        return ApiResponse.error(e.getErrorCode().getStatus().value(), e.getMessage());
    }

    @ExceptionHandler(ChatException.class)
    public ResponseEntity<ApiResponse<String>> handleChatException(ChatException e) {
        log.error("ChatException: {}", e.getMessage());
        return ApiResponse.error(e.getErrorCode().getStatus().value(), e.getMessage());
    }
}

