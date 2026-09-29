package com.yongjibus.chat.repository;

import com.yongjibus.chat.domain.FCMToken;
import com.yongjibus.member.domain.Member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.List;

@Repository
public interface FCMTokenRepository extends JpaRepository<FCMToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from FCMToken token where token.token = :token")
    List<FCMToken> findAllByTokenForUpdate(@Param("token") String token);

    List<FCMToken> findAllByMember(Member member);

    List<FCMToken> findAllByMemberAndIsActiveTrue(Member member);
}
