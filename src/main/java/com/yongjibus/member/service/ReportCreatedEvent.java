package com.yongjibus.member.service;

public record ReportCreatedEvent(
    String reportedUsername,
    String reason,
    String reporterUsername,
    Long roomId
) {
}
