package com.yongjibus.auth.email;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

@Component
public class InMemoryEmailPendingRepository implements EmailPendingRepository {

  private final Set<String> emailPendingSet = ConcurrentHashMap.newKeySet();

  @Override
  public void saveEmailPending(String email) {
    emailPendingSet.add(email);
  }

  @Override
  public void deleteEmailPending(String email) {
    emailPendingSet.remove(email);
  }

  @Override
  public boolean isEmailPending(String email) {
    return emailPendingSet.contains(email);
  }
}
