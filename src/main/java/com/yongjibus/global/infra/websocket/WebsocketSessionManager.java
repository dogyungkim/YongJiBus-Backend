package com.yongjibus.global.infra.websocket;

import java.util.Map;
import java.util.Set;

public interface WebsocketSessionManager {
  void addSession(String sessionId, String email);
  void removeSessionBySessionId(String sessionId);
  void removeSessionByEmail(String email);
  boolean isSessionExists(String email);
  Map<String, Set<String>> getSessions();
}
