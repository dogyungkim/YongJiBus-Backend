package com.yongjibus.chat.repository;

import com.yongjibus.chat.domain.FCMToken;
import com.yongjibus.member.domain.Member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FCMTokenRepository extends JpaRepository<FCMToken, Long> {
    Optional<FCMToken> findByMember(Member member);

    Optional<FCMToken> findByMemberAndIsActiveTrue(Member member);

    Optional<FCMToken> findByTokenAndIsActiveTrue(String token);
}
