package com.yongjibus.global.error.exception;

import org.springframework.messaging.MessageDeliveryException;

import com.yongjibus.global.error.code.ErrorCode;

import lombok.Getter;

@Getter
public class StompException extends MessageDeliveryException {

  private final ErrorCode errorCode;

  public StompException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }
}
