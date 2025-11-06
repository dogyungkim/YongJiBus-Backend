package com.yongjibus.auth.email;

public interface EmailPendingRepository {
  void saveEmailPending(String email);
  void setEmailPendingStatus(String email, boolean isPending);
  void deleteEmailPending(String email);
  boolean isEmailPending(String email);
}