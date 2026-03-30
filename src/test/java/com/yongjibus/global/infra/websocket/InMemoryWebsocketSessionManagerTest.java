package com.yongjibus.global.infra.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryWebsocketSessionManagerTest {

    private final InMemoryWebsocketSessionManager sessionManager = new InMemoryWebsocketSessionManager();

    @Test
    @DisplayName("같은 이메일로 여러 세션이 연결되어도 하나를 끊으면 나머지 세션은 유지된다")
    void removeSessionBySessionId_WhenMultipleSessionsExistForEmail_ShouldKeepOtherSessions() {
        // given
        sessionManager.addSession("session-1", "member@example.com");
        sessionManager.addSession("session-2", "member@example.com");

        // when
        sessionManager.removeSessionBySessionId("session-1");

        // then
        assertThat(sessionManager.isSessionExists("member@example.com")).isTrue();
        assertThat(sessionManager.getSessions().get("member@example.com"))
                .containsExactly("session-2");
    }

    @Test
    @DisplayName("마지막 세션까지 끊기면 온라인 상태도 해제된다")
    void removeSessionBySessionId_WhenLastSessionRemoved_ShouldClearPresence() {
        // given
        sessionManager.addSession("session-1", "member@example.com");
        sessionManager.addSession("session-2", "member@example.com");

        // when
        sessionManager.removeSessionBySessionId("session-1");
        sessionManager.removeSessionBySessionId("session-2");

        // then
        assertThat(sessionManager.isSessionExists("member@example.com")).isFalse();
        assertThat(sessionManager.getSessions()).doesNotContainKey("member@example.com");
    }
}
