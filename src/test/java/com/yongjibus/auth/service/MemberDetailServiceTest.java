package com.yongjibus.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.AuthException;
import com.yongjibus.member.domain.Member;
import com.yongjibus.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class MemberDetailServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberDetailService memberDetailService;

    @Test
    @DisplayName("삭제된 회원은 인증 사용자로 로드할 수 없다")
    void loadUserByUsernameShouldRejectDeletedMember() {
        // given
        Member deletedMember = Member.builder()
                .email("deleted@example.com")
                .password("password123")
                .username("deleted-user")
                .name("삭제회원")
                .build();
        deletedMember.delete();

        when(memberRepository.findByEmail("deleted@example.com"))
                .thenReturn(Optional.of(deletedMember));

        // when & then
        assertThatThrownBy(() -> memberDetailService.loadUserByUsername("deleted@example.com"))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_DELETED);
    }
}
