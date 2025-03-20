package com.yongjibus.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yongjibus.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    Boolean existsByEmail(String email);
    Boolean existsByUsername(String username);
    Optional<Member> findByUsername(String username);
}