package com.yongjibus.auth.email;

import java.math.BigInteger;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gmail_history_checkpoint")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GmailHistoryCheckpoint {

    public static final String CHECKPOINT_KEY = "gmail-bounce";

    @Id
    @Column(name = "checkpoint_key", nullable = false, length = 64)
    private String checkpointKey;

    @Column(name = "last_history_id", nullable = false, precision = 39, scale = 0)
    private BigInteger lastHistoryId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private GmailHistoryCheckpoint(String checkpointKey, BigInteger lastHistoryId) {
        this.checkpointKey = checkpointKey;
        this.lastHistoryId = lastHistoryId;
        this.updatedAt = LocalDateTime.now();
    }

    public static GmailHistoryCheckpoint initialize(BigInteger lastHistoryId) {
        return new GmailHistoryCheckpoint(CHECKPOINT_KEY, lastHistoryId);
    }

    public void advanceTo(BigInteger lastHistoryId) {
        this.lastHistoryId = lastHistoryId;
        this.updatedAt = LocalDateTime.now();
    }
}
