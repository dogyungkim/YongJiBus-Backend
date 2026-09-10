CREATE TABLE member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(10) NOT NULL COMMENT 'real name',
    username VARCHAR(10) NOT NULL UNIQUE COMMENT 'nickname',
    password VARCHAR(255) NOT NULL COMMENT 'encrypted password',
    email VARCHAR(20) NOT NULL UNIQUE COMMENT 'email',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'deleted flag',
    created_at DATETIME(6) COMMENT 'created at',
    updated_at DATETIME(6) COMMENT 'updated at'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='members';

CREATE TABLE arrival_time (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    time_id INT NOT NULL COMMENT 'time ID',
    date DATE NOT NULL COMMENT 'date',
    time TIME NOT NULL COMMENT 'time',
    day_of_week VARCHAR(10) NOT NULL COMMENT 'day of week',
    is_holiday BOOLEAN NOT NULL COMMENT 'holiday flag',
    created_at DATETIME(6) COMMENT 'created at',
    INDEX idx_date_holiday (date, is_holiday)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='arrival times';

CREATE TABLE chat_room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL COMMENT 'room name',
    departure_time TIME NOT NULL COMMENT 'departure time',
    created_at DATETIME(6) COMMENT 'created at'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='chat rooms';

CREATE TABLE chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_type ENUM('MESSAGE', 'ENTER', 'LEAVE', 'SYSTEM') NOT NULL COMMENT 'message type',
    content TEXT NOT NULL COMMENT 'content',
    sender VARCHAR(255) NOT NULL COMMENT 'sender',
    room_id BIGINT NOT NULL COMMENT 'room ID',
    created_at DATETIME(6) COMMENT 'created at',
    INDEX idx_room_created (room_id, created_at DESC),
    FOREIGN KEY (room_id) REFERENCES chat_room(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='chat messages';

CREATE TABLE chat_room_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL COMMENT 'member ID',
    chat_room_id BIGINT NOT NULL COMMENT 'chat room ID',
    joined_at DATETIME(6) COMMENT 'joined at',
    active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'active flag',
    UNIQUE KEY uk_member_chatroom (member_id, chat_room_id),
    INDEX idx_chat_room_active (chat_room_id, active),
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    FOREIGN KEY (chat_room_id) REFERENCES chat_room(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='chat room members';

CREATE TABLE fcmtoken (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL COMMENT 'FCM token',
    member_id BIGINT NOT NULL COMMENT 'member ID',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'active flag',
    last_updated_at DATETIME(6) COMMENT 'last updated at',
    UNIQUE KEY uk_fcmtoken_member (member_id),
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='FCM tokens';

CREATE TABLE member_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reported_member_id BIGINT COMMENT 'reported member ID',
    reason VARCHAR(500) NOT NULL COMMENT 'reason',
    room_id BIGINT NOT NULL COMMENT 'room ID',
    reporter_id BIGINT COMMENT 'reporter ID',
    FOREIGN KEY (reported_member_id) REFERENCES member(id) ON DELETE SET NULL,
    FOREIGN KEY (reporter_id) REFERENCES member(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='member reports';

CREATE TABLE vacation_period (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    start_date DATE COMMENT 'start date',
    end_date DATE COMMENT 'end date',
    vacation_description VARCHAR(500) COMMENT 'description'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='vacation periods';
