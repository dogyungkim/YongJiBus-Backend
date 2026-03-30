package com.yongjibus.chat.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yongjibus.chat.domain.ChatRoom;
import com.yongjibus.chat.domain.ChatRoomMember;
import com.yongjibus.member.domain.Member;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    
    Optional<ChatRoomMember> findByMemberAndChatRoomAndActiveTrue(Member member, ChatRoom chatRoom);
    
    @Query("SELECT crm.joinedAt FROM ChatRoomMember crm WHERE crm.member = :member AND crm.chatRoom = :chatRoom AND crm.active = true")
    Optional<LocalDateTime> findJoinedAtByMemberAndChatRoom(@Param("member") Member member, @Param("chatRoom") ChatRoom chatRoom);
    
    @Query("SELECT crm.chatRoom FROM ChatRoomMember crm WHERE crm.member = :member AND crm.active = true")
    List<ChatRoom> findActiveChatRoomsByMember(@Param("member") Member member);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.id NOT IN " +
           "(SELECT crm.chatRoom.id FROM ChatRoomMember crm WHERE crm.member = :member AND crm.active = true)")
    List<ChatRoom> findChatRoomsNotJoinedByMember(@Param("member") Member member);

    long countByChatRoom_IdAndActiveTrue(Long chatRoomId);
} 
