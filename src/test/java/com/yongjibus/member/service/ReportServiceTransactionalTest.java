package com.yongjibus.member.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.AuthException;
import com.yongjibus.global.infra.email.EmailService;
import com.yongjibus.member.domain.Member;
import com.yongjibus.member.domain.MemberReport;
import com.yongjibus.member.repository.MemberReportRepository;
import com.yongjibus.member.repository.MemberRepository;

@SpringJUnitConfig(ReportServiceTransactionalTest.TestConfig.class)
class ReportServiceTransactionalTest {

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {

        private final ThreadLocal<Boolean> transactionActive = ThreadLocal.withInitial(() -> false);

        @Bean
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override
                protected Object doGetTransaction() {
                    return new Object();
                }

                @Override
                protected boolean isExistingTransaction(Object transaction) {
                    return transactionActive.get();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) {
                    transactionActive.set(true);
                }

                @Override
                protected void doCommit(DefaultTransactionStatus status) {
                    transactionActive.set(false);
                }

                @Override
                protected void doRollback(DefaultTransactionStatus status) {
                    transactionActive.set(false);
                }
            };
        }

        @Bean
        MemberRepository memberRepository() {
            return mock(MemberRepository.class);
        }

        @Bean
        MemberReportRepository memberReportRepository() {
            return mock(MemberReportRepository.class);
        }

        @Bean
        EmailService emailService() {
            return mock(EmailService.class);
        }

        @Bean
        ReportEmailPublisher reportEmailPublisher(ApplicationEventPublisher applicationEventPublisher) {
            return new ReportEmailPublisher(applicationEventPublisher);
        }

        @Bean
        ReportEmailDeliveryService reportEmailDeliveryService(EmailService emailService) {
            return new ReportEmailDeliveryService(emailService);
        }

        @Bean
        ReportService reportService(
                MemberReportRepository memberReportRepository,
                MemberRepository memberRepository,
                ReportEmailPublisher reportEmailPublisher
        ) {
            return new ReportService(memberReportRepository, memberRepository, reportEmailPublisher);
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private ReportService reportService;

    @org.springframework.beans.factory.annotation.Autowired
    private MemberRepository memberRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private MemberReportRepository memberReportRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private EmailService emailService;

    @org.springframework.beans.factory.annotation.Autowired
    private PlatformTransactionManager transactionManager;

    private Member reportedMember;
    private MemberReport memberReport;

    @BeforeEach
    void setUp() {
        reset(memberRepository, memberReportRepository, emailService);

        reportedMember = Member.builder()
                .id(2L)
                .username("reported")
                .build();

        memberReport = MemberReport.builder()
                .reason("욕설")
                .roomId(1L)
                .reporter(Member.builder().id(1L).username("reporter").build())
                .build();

        when(memberRepository.findByUsername("reported")).thenReturn(Optional.of(reportedMember));
        when(memberReportRepository.save(any(MemberReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("커밋 후 메일 전송이 실패해도 신고 저장 성공 의미는 유지된다")
    void createReport_WhenAfterCommitEmailFails_ShouldNotThrow() {
        // given
        doThrow(new AuthException(ErrorCode.EMAIL_SEND_FAILED))
                .when(emailService).sendReportEmail("reported", "욕설", "reporter", 1L);

        // when & then
        assertThatCode(() -> reportService.createReport(memberReport, "reported"))
                .doesNotThrowAnyException();

        verify(memberReportRepository).save(memberReport);
        verify(emailService).sendReportEmail("reported", "욕설", "reporter", 1L);
    }

    @Test
    @DisplayName("외부 트랜잭션이 롤백되면 신고 메일은 전송되지 않는다")
    void createReport_WhenOuterTransactionRollsBack_ShouldNotSendEmail() {
        // given
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        // when
        transactionTemplate.executeWithoutResult(status -> {
            reportService.createReport(memberReport, "reported");
            verify(memberReportRepository).save(memberReport);
            verifyNoInteractions(emailService);
            status.setRollbackOnly();
        });

        // then
        verifyNoInteractions(emailService);
    }
}
