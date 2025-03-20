package com.yongjibus.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.yongjibus.member.domain.Member;
import com.yongjibus.member.repository.MemberRepository;

@DataJpaTest
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PASSWORD = "password123";
    private final String TEST_USERNAME = "testuser";
    private final String TEST_NAME = "테스트유저";

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("회원 저장 및 조회 테스트")
    void saveMemberTest() {
        // given
        Member member = Member.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .username(TEST_USERNAME)
                .name(TEST_NAME)
                .build();

        // when
        Member savedMember = memberRepository.save(member);

        // then
        assertThat(savedMember).isNotNull();
        assertThat(savedMember.getId()).isNotNull();
        assertThat(savedMember.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(savedMember.getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(savedMember.getName()).isEqualTo(TEST_NAME);
        assertThat(savedMember.getPassword()).isEqualTo(TEST_PASSWORD);
    }

    @Test
    @DisplayName("이메일로 회원 조회 테스트 - 존재하는 경우")
    void findByEmailExistsTest() {
        // given
        Member member = Member.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .username(TEST_USERNAME)
                .name(TEST_NAME)
                .build();
        memberRepository.save(member);

        // when
        Optional<Member> foundMember = memberRepository.findByEmail(TEST_EMAIL);

        // then
        assertThat(foundMember).isPresent();
        assertThat(foundMember.get().getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(foundMember.get().getUsername()).isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("이메일로 회원 조회 테스트 - 존재하지 않는 경우")
    void findByEmailNotExistsTest() {
        // given
        String nonExistentEmail = "nonexistent@example.com";

        // when
        Optional<Member> foundMember = memberRepository.findByEmail(nonExistentEmail);

        // then
        assertThat(foundMember).isEmpty();
    }

    @Test
    @DisplayName("이메일 존재 여부 확인 테스트 - 존재하는 경우")
    void existsByEmailTrueTest() {
        // given
        Member member = Member.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .username(TEST_USERNAME)
                .name(TEST_NAME)
                .build();
        memberRepository.save(member);

        // when
        boolean exists = memberRepository.existsByEmail(TEST_EMAIL);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("이메일 존재 여부 확인 테스트 - 존재하지 않는 경우")
    void existsByEmailFalseTest() {
        // given
        String nonExistentEmail = "nonexistent@example.com";

        // when
        boolean exists = memberRepository.existsByEmail(nonExistentEmail);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("사용자명 존재 여부 확인 테스트 - 존재하는 경우")
    void existsByUsernameTrueTest() {
        // given
        Member member = Member.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .username(TEST_USERNAME)
                .name(TEST_NAME)
                .build();
        memberRepository.save(member);

        // when
        boolean exists = memberRepository.existsByUsername(TEST_USERNAME);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("사용자명 존재 여부 확인 테스트 - 존재하지 않는 경우")
    void existsByUsernameFalseTest() {
        // given
        String nonExistentUsername = "nonexistentuser";

        // when
        boolean exists = memberRepository.existsByUsername(nonExistentUsername);

        // then
        assertThat(exists).isFalse();
    }
} 