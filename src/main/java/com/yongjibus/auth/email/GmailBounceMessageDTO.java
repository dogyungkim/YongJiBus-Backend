package com.yongjibus.auth.email;

public record GmailBounceMessageDTO(
    Message message,
    String subscription
) {
    public record Message(
        BounceMessageData data,
        String attributes,
        String messageId,
        String publishTime
    ) {}

    public record BounceMessageData(
        String emailAddress,
        String historyId
    ) {}
}
