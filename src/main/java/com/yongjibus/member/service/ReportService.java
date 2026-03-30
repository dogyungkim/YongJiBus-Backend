package com.yongjibus.member.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.MemberException;
import com.yongjibus.member.domain.Member;
import com.yongjibus.member.domain.MemberReport;
import com.yongjibus.member.repository.MemberReportRepository;
import com.yongjibus.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

  private final MemberReportRepository memberReportRepository;
  private final MemberRepository memberRepository;
  private final ReportEmailPublisher reportEmailPublisher;

  @Transactional
  public void createReport(MemberReport memberReport, String reportedUsername) {
    Optional<Member> reportedMember = memberRepository.findByUsername(reportedUsername);

    if (reportedMember.isEmpty()) {
      throw new MemberException(ErrorCode.REPORT_TARGET_NOT_FOUND);
    }

    memberReport.setReportedMember(reportedMember.get());
    memberReportRepository.save(memberReport);
    reportEmailPublisher.publishAfterCommit(memberReport);
  }
}
