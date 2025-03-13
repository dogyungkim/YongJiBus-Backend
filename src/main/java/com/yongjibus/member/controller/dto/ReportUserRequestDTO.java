package com.yongjibus.member.controller.dto;

public record ReportUserRequestDTO(
  Long reportedUserId,
  String reportedUsername,
  String reason,
  Long roomId
) {
  
}
