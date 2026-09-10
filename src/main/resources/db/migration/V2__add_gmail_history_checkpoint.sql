CREATE TABLE IF NOT EXISTS gmail_history_checkpoint (
    checkpoint_key VARCHAR(64) PRIMARY KEY,
    last_history_id DECIMAL(39,0) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);
