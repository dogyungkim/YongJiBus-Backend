package com.yongjibus.chat.domain;

import com.yongjibus.auth.domain.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Getter
@NoArgsConstructor
public class FCMToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String token;
    
    @OneToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    @Column(name = "is_active")
    private boolean isActive = true;
    
    @LastModifiedDate
    private LocalDateTime lastUpdatedAt;
    
    @Builder
    public FCMToken(String token, Member member) {
        this.token = token;
        this.member = member;
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    public void deactivate() {
        this.isActive = false;
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    public void updateToken(String token) {
        this.token = token;
        this.lastUpdatedAt = LocalDateTime.now();
    }
} 