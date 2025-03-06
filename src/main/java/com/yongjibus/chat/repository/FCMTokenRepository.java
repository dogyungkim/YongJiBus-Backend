package com.yongjibus.chat.repository;

import com.yongjibus.auth.domain.Member;
import com.yongjibus.chat.domain.FCMToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FCMTokenRepository extends JpaRepository<FCMToken, Long> {
    Optional<FCMToken> findByMemberAndIsActiveTrue(Member member);
    Optional<FCMToken> findByTokenAndIsActiveTrue(String token);
} 