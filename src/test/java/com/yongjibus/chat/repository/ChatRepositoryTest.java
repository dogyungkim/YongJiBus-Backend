package com.yongjibus.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import com.yongjibus.chat.domain.ChatMessage;

@DataJpaTest
class ChatRepositoryTest {

    @Autowired
    private ChatRepository chatRepository;

    @BeforeEach
    void setUp() {
        chatRepository.deleteAll();
    }

    @Test
    @DisplayName("입장 이후 메시지만 createdAt 내림차순으로 조회한다")
    void findMessagesAfterJoinTime_ShouldReturnMessagesAfterJoinTimeInDescendingOrder() {
        // given
        chatRepository.save(ChatMessage.builder()
                .messageType(ChatMessage.MessageType.MESSAGE)
                .content("before-join")
                .sender("sender")
                .roomId(1L)
                .createdAt(LocalDateTime.of(2026, 3, 30, 8, 0))
                .build());
        chatRepository.save(ChatMessage.builder()
                .messageType(ChatMessage.MessageType.MESSAGE)
                .content("after-join-1")
                .sender("sender")
                .roomId(1L)
                .createdAt(LocalDateTime.of(2026, 3, 30, 9, 30))
                .build());
        chatRepository.save(ChatMessage.builder()
                .messageType(ChatMessage.MessageType.MESSAGE)
                .content("after-join-2")
                .sender("sender")
                .roomId(1L)
                .createdAt(LocalDateTime.of(2026, 3, 30, 10, 0))
                .build());
        chatRepository.save(ChatMessage.builder()
                .messageType(ChatMessage.MessageType.MESSAGE)
                .content("other-room")
                .sender("sender")
                .roomId(2L)
                .createdAt(LocalDateTime.of(2026, 3, 30, 10, 30))
                .build());

        // when
        Slice<ChatMessage> result = chatRepository.findMessagesAfterJoinTime(
                1L,
                LocalDateTime.of(2026, 3, 30, 9, 0),
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent())
                .extracting(ChatMessage::getContent)
                .containsExactly("after-join-2", "after-join-1");
    }
}
