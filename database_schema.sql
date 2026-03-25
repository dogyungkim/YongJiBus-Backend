-- =============================================
-- YongJiBus Backend Database Schema
-- MySQL 8.0+ 호환
-- =============================================

-- 데이터베이스 생성 (필요시)
-- CREATE DATABASE IF NOT EXISTS yongjibus CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE yongjibus;
-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS yongjibus CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 데이터베이스 선택
USE yongjibus;
-- =============================================
-- 1. Member 테이블 (회원 정보)
-- =============================================
CREATE TABLE member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(10) NOT NULL COMMENT '실제 이름',
    username VARCHAR(10) NOT NULL UNIQUE COMMENT '닉네임',
    password VARCHAR(255) NOT NULL COMMENT '암호화된 비밀번호',
    email VARCHAR(20) NOT NULL UNIQUE COMMENT '이메일',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '삭제 여부',
    created_at DATETIME(6) COMMENT '생성일시',
    updated_at DATETIME(6) COMMENT '수정일시'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 정보';

-- =============================================
-- 2. ArrivalTime 테이블 (도착 시간 정보)
-- =============================================
CREATE TABLE arrival_time (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    time_id INT NOT NULL COMMENT '시간 ID',
    date DATE NOT NULL COMMENT '날짜',
    time TIME NOT NULL COMMENT '시간',
    day_of_week VARCHAR(10) NOT NULL COMMENT '요일',
    is_holiday BOOLEAN NOT NULL COMMENT '휴일 여부',
    created_at DATETIME(6) COMMENT '생성일시',
    
    INDEX idx_date_holiday (date, is_holiday)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='도착 시간 정보';

-- =============================================
-- 3. ChatRoom 테이블 (채팅방 정보)
-- =============================================
CREATE TABLE chat_room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL COMMENT '채팅방 이름',
    departure_time TIME NOT NULL COMMENT '출발 시간',
    created_at DATETIME(6) COMMENT '생성일시'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='채팅방 정보';

-- =============================================
-- 4. ChatMessage 테이블 (채팅 메시지)
-- =============================================
CREATE TABLE chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_type ENUM('MESSAGE', 'ENTER', 'LEAVE', 'SYSTEM') NOT NULL COMMENT '메시지 타입',
    content TEXT NOT NULL COMMENT '메시지 내용',
    sender VARCHAR(255) NOT NULL COMMENT '발신자',
    room_id BIGINT NOT NULL COMMENT '채팅방 ID',
    created_at DATETIME(6) COMMENT '생성일시',
    
    INDEX idx_room_created (room_id, created_at DESC),
    FOREIGN KEY (room_id) REFERENCES chat_room(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='채팅 메시지';

-- =============================================
-- 5. ChatRoomMember 테이블 (채팅방 멤버)
-- =============================================
CREATE TABLE chat_room_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL COMMENT '회원 ID',
    chat_room_id BIGINT NOT NULL COMMENT '채팅방 ID',
    joined_at DATETIME(6) COMMENT '참여일시',
    active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '활성 상태',
    
    UNIQUE KEY uk_member_chatroom (member_id, chat_room_id),
    INDEX idx_chat_room_active (chat_room_id, active),
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    FOREIGN KEY (chat_room_id) REFERENCES chat_room(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='채팅방 멤버';

-- =============================================
-- 6. FCMToken 테이블 (FCM 토큰)
-- =============================================
CREATE TABLE fcmtoken (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL COMMENT 'FCM 토큰',
    member_id BIGINT NOT NULL COMMENT '회원 ID',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '활성 상태',
    last_updated_at DATETIME(6) COMMENT '마지막 업데이트일시',
    
    UNIQUE KEY uk_member_token (member_id, token),
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='FCM 토큰';

-- =============================================
-- 7. MemberReport 테이블 (회원 신고)
-- =============================================
CREATE TABLE member_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reported_member_id BIGINT COMMENT '신고당한 회원 ID',
    reason VARCHAR(500) NOT NULL COMMENT '신고 사유',
    room_id BIGINT NOT NULL COMMENT '채팅방 ID',
    reporter_id BIGINT COMMENT '신고자 ID',
    
    FOREIGN KEY (reported_member_id) REFERENCES member(id) ON DELETE SET NULL,
    FOREIGN KEY (reporter_id) REFERENCES member(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 신고';

-- =============================================
-- 8. VacationPeriod 테이블 (휴가 기간)
-- =============================================
CREATE TABLE vacation_period (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    start_date DATE COMMENT '시작일',
    end_date DATE COMMENT '종료일',
    vacation_description VARCHAR(500) COMMENT '휴가 설명'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='휴가 기간';

-- =============================================
-- 핵심 인덱스만 유지
-- =============================================
-- 실제 사용 패턴에 따라 필요시 추가 인덱스 생성

-- =============================================
-- 테이블 생성 완료 메시지
-- =============================================
SELECT 'YongJiBus Database Schema created successfully!' as message;
