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

    // 채팅방 엔티티가 없는 호출부에서는 roomId로 최신 수신자 스냅샷을 다시 조회한다.
    public void publishAfterCommitByRoomId(ChatMessage message, Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        publishAfterCommitWithRoom(message, chatRoom);
    }

    // 이미 채팅방 엔티티를 가진 호출부는 추가 조회 없이 그대로 이벤트를 발행한다.
    public void publishAfterCommitWithRoom(ChatMessage message, ChatRoom chatRoom) {
        applicationEventPublisher.publishEvent(new ChatMessageDeliveryEvent(
            message,
            chatRoom.getId(),
            chatRoom.getName(),
            chatRoom.getMembers()
        ));
    }
}
