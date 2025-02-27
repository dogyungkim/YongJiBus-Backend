package com.yongjibus.chatting.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yongjibus.chatting.domain.ChatRoom;

public interface ChatRepository extends JpaRepository<ChatRoom, Long> {
    
}
