package com.yongjibus.member.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.yongjibus.auth.service.EmailService;
import com.yongjibus.member.domain.MemberReport;
import com.yongjibus.member.repository.MemberReportRepository;
import com.yongjibus.auth.repository.MemberRepository;
import com.yongjibus.auth.domain.Member;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

  private final EmailService emailService;
  private final MemberReportRepository memberReportRepository;
  private final MemberRepository memberRepository;

  public void createReport(MemberReport memberReport, String reportedUsername) {
    Optional<Member> reportedMember = memberRepository.findByUsername(reportedUsername);

    if (reportedMember.isEmpty()) {
      throw new RuntimeException("신고 대상 멤버가 존재하지 않습니다.");
    }

    memberReport.setReportedMember(reportedMember.get());
    // 신고 처리
    memberReportRepository.save(memberReport);
    // 신고 이메일 전송
    emailService.sendReportEmail(memberReport);
  }
}
