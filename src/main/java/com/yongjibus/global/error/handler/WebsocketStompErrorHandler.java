package com.yongjibus.global.error.handler;

import java.nio.charset.StandardCharsets;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.StompException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WebsocketStompErrorHandler extends StompSubProtocolErrorHandler {

  @Override
  public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
    if (ex instanceof StompException) {
      log.error("StompException: {}", ex.getMessage());
      StompException stompException = (StompException) ex;
      ErrorCode errorCode = stompException.getErrorCode();
      return createErrorMessage(errorCode);
    }
    return super.handleClientMessageProcessingError(clientMessage, ex);
  }
  
  @Override
  protected Message<byte[]> handleInternal(StompHeaderAccessor errorHeaderAccessor, 
                                          byte[] errorPayload, Throwable cause, StompHeaderAccessor clientHeaderAccessor) {
      return super.handleInternal(errorHeaderAccessor, errorPayload, cause, clientHeaderAccessor);
  }
  
  private Message<byte[]> createErrorMessage(ErrorCode errorCode){
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      String json = objectMapper.writeValueAsString(errorCode.getMessage());

      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
      accessor.setContentType(MimeType.valueOf("application/json"));
      accessor.setMessage(json);
      accessor.setLeaveMutable(true);
      
      log.info("createErrorMessage: {}", json);

      return MessageBuilder.createMessage(json.getBytes(StandardCharsets.UTF_8), accessor.getMessageHeaders());
  } catch (JsonProcessingException e) {
      // JSON 변환 실패 시 기본 에러 메시지 반환
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
      accessor.setMessage("Error processing error message");
      accessor.setLeaveMutable(true);
      
      String fallbackMessage = "{\"status\":" + errorCode.getStatus() + 
                            ",\"message\":\"" + errorCode.getMessage() + "\"}";
      
      return MessageBuilder.createMessage(
          fallbackMessage.getBytes(StandardCharsets.UTF_8), 
          accessor.getMessageHeaders());
    }
  }
}