package com.yongjibus.chat.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import com.yongjibus.chat.domain.ChatMessage;

public interface ChatRepository extends JpaRepository<ChatMessage, Long> {
    Slice<ChatMessage> findByRoomId(Long roomId, Pageable pageable);
    Slice<ChatMessage> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);
}
