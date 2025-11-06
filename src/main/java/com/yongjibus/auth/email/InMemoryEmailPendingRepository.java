package com.yongjibus.auth.email;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
public class InMemoryEmailPendingRepository implements EmailPendingRepository {

  private final Map<String, Boolean> emailPendingMap = new ConcurrentHashMap<>();

  @Override
  public void saveEmailPending(String email) {
    emailPendingMap.put(email,false);
  } 

  @Override
  public void setEmailPendingStatus(String email, boolean isPending) {
    if (!emailPendingMap.containsKey(email)) {
      throw new IllegalStateException("Email key does not exist: " + email);
    }
    emailPendingMap.put(email, isPending);
  }

  @Override
  public void deleteEmailPending(String email) {
    emailPendingMap.remove(email);
  }

  @Override
  public boolean isEmailPending(String email) {
    return emailPendingMap.getOrDefault(email, false);
  }
}