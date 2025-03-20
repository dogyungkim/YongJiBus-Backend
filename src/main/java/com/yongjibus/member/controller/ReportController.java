package com.yongjibus.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yongjibus.auth.domain.MemberDetail;
import com.yongjibus.global.common.response.ApiResponse;
import com.yongjibus.member.controller.dto.ReportUserRequestDTO;
import com.yongjibus.member.domain.Member;
import com.yongjibus.member.domain.MemberReport;
import com.yongjibus.member.service.ReportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

  private final ReportService reportService;

  @PostMapping("/user")
  public ResponseEntity<ApiResponse<String>> reportUser(
    @RequestBody ReportUserRequestDTO request,
    @AuthenticationPrincipal MemberDetail memberDetail
  ) {
    Member reporter = memberDetail.getMember();

    MemberReport userReport = MemberReport.builder()
      .reason(request.reason())
      .roomId(request.roomId())
      .reporter(reporter)
      .build();

    //TODO : 비동기 처리로 하는것이 빠를거같다.
    reportService.createReport(userReport, request.reportedUserName());

    return ApiResponse.success("success");
  }
}
