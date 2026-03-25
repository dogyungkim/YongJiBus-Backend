package com.yongjibus.member.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.AuthException;
import com.yongjibus.member.domain.Member;
import com.yongjibus.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("이메일 유니크 제약 위반은 EMAIL_ALREADY_EXISTS로 변환한다")
    void saveMemberShouldTranslateDuplicateEmailException() {
        // given
        Member member = Member.builder()
                .email("test@example.com")
                .password("password123")
                .username("testuser")
                .name("테스트")
                .build();

        when(memberRepository.saveAndFlush(any(Member.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate email"));
        when(memberRepository.existsByEmail(member.getEmail())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.saveMember(member))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);
    }
}
