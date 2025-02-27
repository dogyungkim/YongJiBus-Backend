package com.yongjibus.chatting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.yongjibus.chatting.domain.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
} 