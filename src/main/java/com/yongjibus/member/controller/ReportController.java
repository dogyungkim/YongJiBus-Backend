package com.yongjibus.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yongjibus.auth.domain.MemberDetail;
import com.yongjibus.global.common.response.YongJiResponse;
import com.yongjibus.member.controller.dto.ReportUserRequestDTO;
import com.yongjibus.member.domain.Member;
import com.yongjibus.member.domain.MemberReport;
import com.yongjibus.member.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "신고 API", description = "사용자 신고 관련 API")
public class ReportController {

  private final ReportService reportService;

  @Operation(
    summary = "사용자 신고하기", 
    description = "문제가 있는 사용자를 신고합니다. 신고 사유와 대상 사용자 정보가 필요합니다. 인증이 필요한 엔드포인트입니다.",
    security = { @SecurityRequirement(name = "Bearer Authentication") }
  )
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "200", 
      description = "신고 접수 성공",
      content = @Content(
        schema = @Schema(type = "object", example = "{\"status\":\"SUCCESS\",\"message\":null,\"data\":\"success\"}")
      )
    ),
    @ApiResponse(
      responseCode = "400", 
      description = "잘못된 요청 데이터", 
      content = @Content
    ),
    @ApiResponse(
      responseCode = "401", 
      description = "인증되지 않은 사용자", 
      content = @Content
    ),
    @ApiResponse(
      responseCode = "404", 
      description = "신고 대상 사용자를 찾을 수 없음", 
      content = @Content
    )
  })
  @PostMapping("/user")
  public ResponseEntity<YongJiResponse<String>> reportUser(
    @Parameter(
      description = "신고 정보", 
      required = true,
      schema = @Schema(implementation = ReportUserRequestDTO.class)
    )
    @RequestBody ReportUserRequestDTO request,
    
    @Parameter(description = "현재 인증된 사용자(신고자)", hidden = true)
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

    return YongJiResponse.success("success");
  }
}
