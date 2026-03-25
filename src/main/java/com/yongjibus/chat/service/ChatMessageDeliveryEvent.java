package com.yongjibus.chat.service;

import java.util.List;

import com.yongjibus.chat.domain.ChatMessage;
import com.yongjibus.member.domain.Member;

public record ChatMessageDeliveryEvent(
    ChatMessage message,
    Long roomId,
    String roomName,
    List<Member> recipients
) {

    public ChatMessageDeliveryEvent {
        recipients = List.copyOf(recipients);
    }
}
