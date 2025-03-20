package com.yongjibus.global.infra.websocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class InMemoryWebsocketSessionManager implements WebsocketSessionManager {
  private final Map<String, String> sessions = new ConcurrentHashMap<>();

  @Override
  public void addSession(String sessionId, String email) {
    log.info("Adding session: {} for email: {}", sessionId, email);
    sessions.put(email, sessionId);
  }

  @Override
  public void removeSessionBySessionId(String sessionId) {
    log.info("Removing session: {}", sessionId);
    sessions.entrySet().removeIf(entry -> entry.getValue().equals(sessionId));
  }

  @Override
  public void removeSessionByEmail(String email) {
    sessions.remove(email);
  }

  @Override
  public boolean isSessionExists(String email) {
    return sessions.containsKey(email);
  }

  @Override
  public Map<String, String> getSessions() {
    return sessions;
  }
}
