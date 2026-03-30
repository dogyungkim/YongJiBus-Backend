package com.yongjibus.member.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.yongjibus.member.domain.MemberReport;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportEmailPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishAfterCommit(MemberReport memberReport) {
        applicationEventPublisher.publishEvent(new ReportCreatedEvent(
            memberReport.getReportedMember().getUsername(),
            memberReport.getReason(),
            memberReport.getReporter().getUsername(),
            memberReport.getRoomId()
        ));
    }
}
