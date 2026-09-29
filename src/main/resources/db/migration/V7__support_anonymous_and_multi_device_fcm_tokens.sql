-- Duplicate tokens cannot be safely attributed to one member, so preserve the rows but disable delivery.
UPDATE fcmtoken
SET is_active = FALSE
WHERE token IN (
    SELECT token FROM (
        SELECT token
        FROM fcmtoken
        GROUP BY token
        HAVING COUNT(*) > 1
    ) duplicate_tokens
);

ALTER TABLE fcmtoken MODIFY token VARCHAR(512) NOT NULL;
ALTER TABLE fcmtoken MODIFY member_id BIGINT NULL;
ALTER TABLE fcmtoken DROP INDEX uk_fcmtoken_member;
ALTER TABLE fcmtoken ADD COLUMN active_token VARCHAR(512) GENERATED ALWAYS AS (
    CASE WHEN is_active THEN token ELSE NULL END
);
CREATE INDEX idx_fcmtoken_token ON fcmtoken (token);
CREATE UNIQUE INDEX uk_fcmtoken_active_token ON fcmtoken (active_token);
