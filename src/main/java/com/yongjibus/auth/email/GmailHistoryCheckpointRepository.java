package com.yongjibus.auth.email;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GmailHistoryCheckpointRepository extends JpaRepository<GmailHistoryCheckpoint, String> {
}
