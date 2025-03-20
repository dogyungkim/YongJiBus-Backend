package com.yongjibus.chat.domain;

import java.time.LocalDateTime;

import com.yongjibus.member.domain.Member;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomMember {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;
    
    private LocalDateTime joinedAt;
    
    private boolean active; // 현재 채팅방에 참여 중인지 여부
    
    public static ChatRoomMember createChatRoomMember(Member member, ChatRoom chatRoom) {
        return ChatRoomMember.builder()
                .member(member)
                .chatRoom(chatRoom)
                .joinedAt(LocalDateTime.now())
                .active(true)
                .build();
    }
    
    public void leave() {
        this.active = false;
    }
    
    public void rejoin() {
        this.active = true;
        this.joinedAt = LocalDateTime.now();
    }
} 