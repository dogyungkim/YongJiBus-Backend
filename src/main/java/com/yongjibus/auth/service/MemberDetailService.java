package com.yongjibus.auth.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.yongjibus.auth.domain.MemberDetail;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.AuthException;
import com.yongjibus.member.domain.Member;
import com.yongjibus.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberDetailService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        if (member.getIsDeleted()) {
            throw new AuthException(ErrorCode.MEMBER_DELETED);
        }

        return new MemberDetail(member);
    }
}
