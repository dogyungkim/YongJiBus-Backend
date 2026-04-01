package com.yongjibus.global.error.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.StompException;

class WebsocketStompErrorHandlerTest {

    private final WebsocketStompErrorHandler errorHandler = new WebsocketStompErrorHandler();

    @Test
    @DisplayName("STOMP 예외는 JSON ERROR frame으로 변환한다")
    void handleClientMessageProcessingError_WhenStompException_ShouldCreateJsonErrorFrame() {
        // when
        Message<byte[]> message = errorHandler.handleClientMessageProcessingError(
                null,
                new StompException(ErrorCode.CHAT_ROOM_FORBIDDEN)
        );

        // then
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        assertThat(accessor.getCommand()).isEqualTo(StompCommand.ERROR);
        assertThat(accessor.getContentType().toString()).isEqualTo("application/json");
        assertThat(new String(message.getPayload(), StandardCharsets.UTF_8))
                .isEqualTo("\"" + ErrorCode.CHAT_ROOM_FORBIDDEN.getMessage() + "\"");
    }
}
