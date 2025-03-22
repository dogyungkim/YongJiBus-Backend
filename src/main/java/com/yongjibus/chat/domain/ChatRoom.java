package com.yongjibus.chat.domain;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yongjibus.member.domain.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(AuditingEntityListener.class) 
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalTime departureTime;
    
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatRoomMember> chatRoomMembers = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public ChatRoom(String name, LocalTime departureTime) {
        this.name = name;
        this.departureTime = departureTime;
        this.createdAt = LocalDateTime.now();
    }

    // 사용자 수를 members 리스트의 크기로 계산하는 메서드
    public int getUserCount() {
        return (int) chatRoomMembers.stream()
                .filter(ChatRoomMember::isActive)
                .count();
    }

    public List<Member> getMembers() {
        return chatRoomMembers.stream()
                .filter(ChatRoomMember::isActive)
                .map(ChatRoomMember::getMember)
                .collect(Collectors.toList());
    }

    public void addMember(Member member) {
        ChatRoomMember chatRoomMember = chatRoomMembers.stream()
                .filter(crm -> crm.getMember().equals(member))
                .findFirst()
                .orElse(null);
                
        if (chatRoomMember == null) {
            chatRoomMembers.add(ChatRoomMember.createChatRoomMember(member, this));
        } else if (!chatRoomMember.isActive()) {
            chatRoomMember.rejoin();
        }
    }

    public void removeMember(Member member) {
        chatRoomMembers.stream()
                .filter(crm -> crm.getMember().equals(member) && crm.isActive())
                .findFirst()
                .ifPresent(ChatRoomMember::leave);
    }
}
