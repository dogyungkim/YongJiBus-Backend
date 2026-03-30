package com.yongjibus.member.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.MemberException;
import com.yongjibus.member.domain.Member;
import com.yongjibus.member.domain.MemberReport;
import com.yongjibus.member.repository.MemberReportRepository;
import com.yongjibus.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private MemberReportRepository memberReportRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ReportEmailPublisher reportEmailPublisher;

    @InjectMocks
    private ReportService reportService;

    @Test
    @DisplayName("신고 대상이 없으면 404 계열 예외를 던진다")
    void createReport_WhenReportedMemberMissing_ShouldThrowMemberException() {
        // given
        MemberReport memberReport = MemberReport.builder()
                .reason("욕설")
                .roomId(1L)
                .reporter(Member.builder().id(1L).username("reporter").build())
                .build();
        when(memberRepository.findByUsername("missing-user")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reportService.createReport(memberReport, "missing-user"))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REPORT_TARGET_NOT_FOUND);
        verifyNoInteractions(memberReportRepository, reportEmailPublisher);
    }

    @Test
    @DisplayName("신고 대상이 있으면 신고를 저장하고 메일 이벤트를 발행한다")
    void createReport_WhenReportedMemberExists_ShouldSaveAndPublishEvent() {
        // given
        Member reportedMember = Member.builder().id(2L).username("reported").build();
        MemberReport memberReport = MemberReport.builder()
                .reason("욕설")
                .roomId(1L)
                .reporter(Member.builder().id(1L).username("reporter").build())
                .build();
        when(memberRepository.findByUsername("reported")).thenReturn(Optional.of(reportedMember));

        // when
        reportService.createReport(memberReport, "reported");

        // then
        verify(memberReportRepository, times(1)).save(memberReport);
        verify(reportEmailPublisher, times(1)).publishAfterCommit(memberReport);
        verify(memberRepository, times(1)).findByUsername("reported");
    }
}
