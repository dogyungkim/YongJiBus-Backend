package com.yongjibus.member.controller.dto;

public record ReportUserRequestDTO(
  String reportedUserName,
  String reason,
  Long roomId
) {
  
}
