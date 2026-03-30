package com.yongjibus.global.infra.websocket;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class InMemoryWebsocketSessionManager implements WebsocketSessionManager {
  private final Map<String, Set<String>> sessions = new ConcurrentHashMap<>();

  @Override
  public void addSession(String sessionId, String email) {
    log.info("Adding session: {} for email: {}", sessionId, email);
    sessions.computeIfAbsent(email, key -> ConcurrentHashMap.newKeySet()).add(sessionId);
  }

  @Override
  public void removeSessionBySessionId(String sessionId) {
    log.info("Removing session: {}", sessionId);
    sessions.forEach((email, sessionIds) -> {
      if (sessionIds.remove(sessionId) && sessionIds.isEmpty()) {
        sessions.remove(email, sessionIds);
      }
    });
  }

  @Override
  public void removeSessionByEmail(String email) {
    sessions.remove(email);
  }

  @Override
  public boolean isSessionExists(String email) {
    Set<String> sessionIds = sessions.get(email);
    return sessionIds != null && !sessionIds.isEmpty();
  }

  @Override
  public Map<String, Set<String>> getSessions() {
    return sessions;
  }
}
