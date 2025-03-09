package com.yongjibus.global.exception;

import org.springframework.messaging.MessageDeliveryException;

import lombok.Getter;

@Getter
public class StompException extends MessageDeliveryException {

  private final ErrorCode errorCode;

  public StompException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }
}
