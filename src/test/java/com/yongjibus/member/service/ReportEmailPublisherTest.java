package com.yongjibus.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.yongjibus.member.domain.Member;
import com.yongjibus.member.domain.MemberReport;

@ExtendWith(MockitoExtension.class)
class ReportEmailPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private ReportEmailPublisher reportEmailPublisher;

    @Test
    @DisplayName("신고 메일 이벤트는 엔티티 대신 스냅샷으로 발행한다")
    void publishAfterCommit_ShouldPublishSnapshotEvent() {
        // given
        MemberReport memberReport = MemberReport.builder()
                .reportedMember(Member.builder().username("reported-user").build())
                .reporter(Member.builder().username("reporter-user").build())
                .reason("욕설")
                .roomId(12L)
                .build();

        // when
        reportEmailPublisher.publishAfterCommit(memberReport);

        // then
        ArgumentCaptor<ReportCreatedEvent> captor = ArgumentCaptor.forClass(ReportCreatedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        ReportCreatedEvent event = captor.getValue();
        assertThat(event.reportedUsername()).isEqualTo("reported-user");
        assertThat(event.reason()).isEqualTo("욕설");
        assertThat(event.reporterUsername()).isEqualTo("reporter-user");
        assertThat(event.roomId()).isEqualTo(12L);
    }
}
