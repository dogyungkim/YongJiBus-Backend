package com.yongjibus.member.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

import com.yongjibus.global.infra.email.EmailService;

@SpringJUnitConfig(ReportEmailDeliveryServiceTransactionalTest.TestConfig.class)
class ReportEmailDeliveryServiceTransactionalTest {

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override
                protected Object doGetTransaction() {
                    return new Object();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) {
                }

                @Override
                protected void doCommit(DefaultTransactionStatus status) {
                }

                @Override
                protected void doRollback(DefaultTransactionStatus status) {
                }
            };
        }

        @Bean
        EmailService emailService() {
            return mock(EmailService.class);
        }

        @Bean
        ReportEmailDeliveryService reportEmailDeliveryService(EmailService emailService) {
            return new ReportEmailDeliveryService(emailService);
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @org.springframework.beans.factory.annotation.Autowired
    private PlatformTransactionManager transactionManager;

    @org.springframework.beans.factory.annotation.Autowired
    private EmailService emailService;

    @Test
    @DisplayName("신고 메일 이벤트는 트랜잭션 커밋 후에만 실행된다")
    void deliver_ShouldRunAfterCommit() {
        // given
        ReportCreatedEvent event = new ReportCreatedEvent("reported-user", "욕설", "reporter-user", 11L);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        // when
        transactionTemplate.executeWithoutResult(status -> {
            applicationEventPublisher.publishEvent(event);
            verifyNoInteractions(emailService);
        });

        // then
        verify(emailService).sendReportEmail("reported-user", "욕설", "reporter-user", 11L);
    }
}
