package com.yongjibus.chat.service;

import com.yongjibus.chat.domain.ChatMessage;
import com.yongjibus.chat.domain.ChatRoom;
import com.yongjibus.chat.repository.ChatRoomRepository;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.ChatException;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMessageDeliveryPublisher {

    private final ChatRoomRepository chatRoomRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishAfterCommit(ChatMessage message, Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        publishAfterCommit(message, chatRoom);
    }

    public void publishAfterCommit(ChatMessage message, ChatRoom chatRoom) {
        applicationEventPublisher.publishEvent(new ChatMessageDeliveryEvent(
            message,
            chatRoom.getId(),
            chatRoom.getName(),
            chatRoom.getMembers()
        ));
    }
}
