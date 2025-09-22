package com.yongjibus.auth.email;

public record GmailBounceMessageDTO(
    GmailBounceDataDTO message
) {
    public record GmailBounceDataDTO(
    String historyId,
    String messageId
){}
}

