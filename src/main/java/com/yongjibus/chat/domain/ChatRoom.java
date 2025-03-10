package com.yongjibus.chat.domain;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;

import com.yongjibus.auth.domain.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
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
    
    @OneToMany(mappedBy = "room")
    private List<Member> members = new ArrayList<>();

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
        return members.size();
    }
}
