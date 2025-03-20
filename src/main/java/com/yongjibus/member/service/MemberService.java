package com.yongjibus.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.AuthException;
import com.yongjibus.member.domain.Member;
import com.yongjibus.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {
    private final MemberRepository memberRepository;

    @Transactional
    public void saveMember(Member member) {
        memberRepository.save(member);
    }

    public Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
            .orElseThrow(() -> new AuthException(ErrorCode.INVALID_CREDENTIALS));
    }

    public void validateMemberInfoToSignup(Member member) {
        validateDuplicateEmail(member.getEmail());
        validateDuplicateUsername(member.getUsername());
    }

    private void validateDuplicateEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new AuthException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    private void validateDuplicateUsername(String username) {
        if (memberRepository.existsByUsername(username)) {
            throw new AuthException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
    }
    
    /**
     * 사용자 이름(username) 중복 여부를 확인합니다.
     * @param username 확인할 사용자 이름
     * @return 중복되면 true, 아니면 false
     */
    @Transactional(readOnly = true)
    public boolean checkUsernameExists(String username) {
        return memberRepository.existsByUsername(username);
    }
}
