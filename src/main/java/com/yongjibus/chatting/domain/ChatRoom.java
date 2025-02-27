package com.yongjibus.chatting.domain;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.yongjibus.auth.domain.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalTime departureTime;
    
    @Column(nullable = false)
    private int userCount;

    @OneToMany(mappedBy = "room")
    private List<Member> members;

    private LocalDateTime createdAt;

    @Builder
    public ChatRoom(String name, LocalTime departureTime, int userCount) {
        this.name = name;
        this.departureTime = departureTime;
        this.userCount = userCount;
        this.createdAt = LocalDateTime.now();
    }
}
