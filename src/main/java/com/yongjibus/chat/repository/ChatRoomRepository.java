package com.yongjibus.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yongjibus.chat.domain.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
} 