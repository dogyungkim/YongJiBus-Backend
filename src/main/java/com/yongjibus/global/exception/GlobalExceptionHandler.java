package com.yongjibus.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DateInfoNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handleNotFoundException(DateInfoNotFoundException e) {
        log.error("DateInfoNotFoundException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ExceptionResponse(e.getMessage(),404));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult()
                .getAllErrors()
                .get(0)
                .getDefaultMessage();
        log.error("MethodArgumentNotValidException: {}", errorMessage);
        return ResponseEntity.badRequest().body(new ExceptionResponse(errorMessage, 400));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ExceptionResponse> handleAuthException(AuthException e) {
        log.error("AuthException: {}", e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(new ExceptionResponse(e.getMessage(), e.getErrorCode().getStatus().value()));
    }
}

