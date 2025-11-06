package com.yongjibus.auth.repository;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import com.yongjibus.auth.email.EmailPendingRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class EmailPendingRepositoryTest {

    @Autowired
    private EmailPendingRepository emailPendingRepository;

    @BeforeEach
    public void setUp() {
        emailPendingRepository.deleteEmailPending("test@example.com");
    }

    @Test
    @DisplayName("이메일 발송 요청 저장 테스트 - 기본값 false")
    public void testSaveEmailPending_ReturnsFalse() {
        emailPendingRepository.saveEmailPending("test@example.com");
        assertThat(emailPendingRepository.isEmailPending("test@example.com")).isFalse();
    }

    @Test   
    @DisplayName("이메일 발송 상태 설정 테스트")
    public void testSetEmailPendingStatus_ReturnsTrue() {
        emailPendingRepository.saveEmailPending("test@example.com");
        emailPendingRepository.setEmailPendingStatus("test@example.com", true);
        assertThat(emailPendingRepository.isEmailPending("test@example.com")).isTrue();
    }

    @Test
    @DisplayName("이메일 발송 상태 설정 테스트 - 이메일 존재하지 않음")
    public void testSetEmailPendingStatus_EmailNotExists() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            emailPendingRepository.setEmailPendingStatus("test@example.com", true)
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("이메일 상태 가져오기 실패 - 이메일 존재하지 않음")
    public void testGetEmailPendingStatus_EmailNotExists() {
        assertThat(emailPendingRepository.isEmailPending("test@example.com")).isFalse();
    }

    @Test
    @DisplayName("이메일 상태 두 번 연속 저장시 기본값 false")
    public void testSaveEmailPending_ReturnsFalse_Twice() {
        emailPendingRepository.saveEmailPending("test@example.com");
        emailPendingRepository.saveEmailPending("test@example.com");
        assertThat(emailPendingRepository.isEmailPending("test@example.com")).isFalse();
    }

    @Test
    @DisplayName("이메일 상태 재설정 시 기본값 false")
    public void testSetEmailPendingStatus_ReturnsFalse_Twice() {
        emailPendingRepository.saveEmailPending("test@example.com");
        emailPendingRepository.setEmailPendingStatus("test@example.com", true);
        emailPendingRepository.saveEmailPending("test@example.com");
        assertThat(emailPendingRepository.isEmailPending("test@example.com")).isFalse();
    }
}
