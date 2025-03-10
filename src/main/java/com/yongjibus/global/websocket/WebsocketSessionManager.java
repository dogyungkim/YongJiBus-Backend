package com.yongjibus.global.websocket;

import java.util.Map;

public interface WebsocketSessionManager {
  void addSession(String sessionId, String email);
  void removeSessionBySessionId(String sessionId);
  void removeSessionByEmail(String email);
  boolean isSessionExists(String sessionId);
  Map<String, String> getSessions();
}
