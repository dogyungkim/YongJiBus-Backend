package com.yongjibus.global.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.yongjibus.global.exception.ErrorCode;
import com.yongjibus.global.exception.StompException;
import com.yongjibus.global.jwt.JwtService;

@Component
@Slf4j
@RequiredArgsConstructor
public class StompPreHandler implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("STOMP Connection established");
            String token = accessor.getNativeHeader("authorization").get(0);
            if (token != null && token.startsWith("Bearer ")) { 
                token = token.substring(7);
                if (jwtService.validateToken(token)) {
                    throw new StompException(ErrorCode.INVALID_ACCESS_TOKEN);
                } else {
                    throw new StompException(ErrorCode.INVALID_ACCESS_TOKEN);
                }
            }
        } else if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            log.info("STOMP Subscription: {}", accessor.getDestination());
        } else if (accessor != null && StompCommand.SEND.equals(accessor.getCommand())) {
            log.info("STOMP Message sent to: {}", accessor.getDestination());
        } else if (accessor != null && StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            log.info("STOMP Connection closed");
        }
        return message;
    }
}
