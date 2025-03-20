package com.yongjibus.chat.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yongjibus.chat.domain.ChatMessage;
import java.time.LocalDateTime;

public interface ChatRepository extends JpaRepository<ChatMessage, Long> {
    Slice<ChatMessage> findByRoomId(Long roomId, Pageable pageable);
    Slice<ChatMessage> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);
    @Query("SELECT c FROM ChatMessage c WHERE c.roomId = :roomId AND c.createdAt >= :joinedAt ORDER BY c.createdAt DESC")
    Slice<ChatMessage> findMessagesAfterJoinTime(@Param("roomId") Long roomId, @Param("joinedAt") LocalDateTime joinedAt, Pageable pageable);
}
