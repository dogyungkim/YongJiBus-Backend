package com.yongjibus.global.error.handler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.yongjibus.global.common.response.YongJiResponse;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.AuthException;
import com.yongjibus.global.error.exception.ChatException;
import com.yongjibus.global.error.exception.DateInfoNotFoundException;
import com.yongjibus.global.error.exception.MemberException;
import com.yongjibus.global.error.exception.VacationException;

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
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse(ErrorCode.INVALID_REQUEST.getMessage());
        log.error("MethodArgumentNotValidException: {}", errorMessage);
        return YongJiResponse.error(ErrorCode.INVALID_REQUEST.getStatus().value(), errorMessage);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<YongJiResponse<String>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        ErrorCode errorCode = isDateOrTimeType(e.getRequiredType())
                ? ErrorCode.INVALID_DATE_FORMAT
                : ErrorCode.INVALID_REQUEST;
        log.error("MethodArgumentTypeMismatchException: {}", e.getMessage());
        return YongJiResponse.error(errorCode.getStatus().value(), errorCode.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<YongJiResponse<String>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        InvalidFormatException invalidFormatException = findCause(e, InvalidFormatException.class);
        ErrorCode errorCode = invalidFormatException != null && isDateOrTimeType(invalidFormatException.getTargetType())
                ? ErrorCode.INVALID_DATE_FORMAT
                : ErrorCode.INVALID_REQUEST;
        log.error("HttpMessageNotReadableException: {}", e.getMessage());
        return YongJiResponse.error(errorCode.getStatus().value(), errorCode.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<YongJiResponse<String>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException e) {
        String errorMessage = e.getParameterName() + " 파라미터는 필수입니다.";
        log.error("MissingServletRequestParameterException: {}", errorMessage);
        return YongJiResponse.error(ErrorCode.INVALID_REQUEST.getStatus().value(), errorMessage);
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

    @ExceptionHandler(MemberException.class)
    public ResponseEntity<YongJiResponse<String>> handleMemberException(MemberException e) {
        log.error("MemberException: {}", e.getMessage());
        return YongJiResponse.error(e.getErrorCode().getStatus().value(), e.getMessage());
    }

    @ExceptionHandler(VacationException.class)
    public ResponseEntity<YongJiResponse<String>> handleVacationException(VacationException e) {
        log.error("VacationException: {}", e.getMessage());
        return YongJiResponse.error(e.getErrorCode().getStatus().value(), e.getMessage());
    }

    private boolean isDateOrTimeType(Class<?> type) {
        return type != null
                && (LocalDate.class.isAssignableFrom(type)
                || LocalDateTime.class.isAssignableFrom(type)
                || LocalTime.class.isAssignableFrom(type));
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return causeType.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
