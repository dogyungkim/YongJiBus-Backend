package com.yongjibus.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yongjibus.chat.domain.ChatMessage;

public interface ChatRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRoomId(Long roomId);
}
