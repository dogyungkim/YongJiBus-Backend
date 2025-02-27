package com.yongjibus.chatting.service;

import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yongjibus.chatting.domain.ChatRoom;
import com.yongjibus.chatting.repository.ChatRoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    
    private final ChatRoomRepository chatRoomRepository;

    @Transactional(readOnly = true)
    public List<ChatRoom> getAllChatRooms() {
        return chatRoomRepository.findAll();
    }

    @Transactional
    public ChatRoom createChatRoom(String name, LocalTime departureTime) {
        ChatRoom chatRoom = ChatRoom.builder()
                .name(name)
                .departureTime(departureTime)
                .userCount(1)
                .build();
        return chatRoomRepository.save(chatRoom);
    }
} 