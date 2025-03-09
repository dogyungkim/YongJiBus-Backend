package com.yongjibus.global.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import com.yongjibus.global.exception.ErrorCode;
import com.yongjibus.global.exception.StompException;
import com.yongjibus.global.jwt.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        String token = request.getHeaders().getFirst("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                if (jwtService.validateToken(token)) {
                    attributes.put("username", jwtService.getEmailFromToken(token));
                    return true;
                }
            } catch (Exception e) {
                log.error("JwtHandshakeInterceptor beforeHandshake error: {}", e.getMessage());
                // iOS에서 해당 핸드 쉐이크 과정에서 따로 예외를 받을 수 없음.
                return false;
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                               WebSocketHandler wsHandler, Exception exception) {
    }
}