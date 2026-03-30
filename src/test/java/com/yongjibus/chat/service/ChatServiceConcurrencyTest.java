package com.yongjibus.chat.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.yongjibus.chat.domain.ChatRoom;
import com.yongjibus.chat.repository.ChatRepository;
import com.yongjibus.chat.repository.ChatRoomMemberRepository;
import com.yongjibus.chat.repository.ChatRoomRepository;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.ChatException;
import com.yongjibus.member.domain.Member;
import com.yongjibus.member.repository.MemberRepository;

@SpringBootTest
class ChatServiceConcurrencyTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ChatRepository chatRepository;

    @MockBean
    private ChatMessageDeliveryPublisher chatMessageDeliveryPublisher;

    @AfterEach
    void tearDown() {
        chatRepository.deleteAll();
        chatRoomMemberRepository.deleteAll();
        chatRoomRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("정원 4명 상태에서 2명이 동시에 참여하면 한 명만 성공하고 정원은 5명을 넘지 않는다")
    void joinChatRoom_WhenConcurrentRequestsArrive_ShouldAllowOnlyOneAdditionalMember() throws Exception {
        // given
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.builder()
                .name("동시성 채팅방")
                .departureTime(LocalTime.of(8, 30))
                .build());

        for (int i = 1; i <= 4; i++) {
            Member existingMember = memberRepository.save(Member.builder()
                    .name("기존회원" + i)
                    .username("exist" + i)
                    .email("e" + i + "@yj.kr")
                    .password("password")
                    .build());
            chatRoom.addMember(existingMember);
        }
        chatRoomRepository.save(chatRoom);

        Member joiner1 = memberRepository.save(Member.builder()
                .name("신규회원1")
                .username("joiner1")
                .email("j1@yj.kr")
                .password("password")
                .build());
        Member joiner2 = memberRepository.save(Member.builder()
                .name("신규회원2")
                .username("joiner2")
                .email("j2@yj.kr")
                .password("password")
                .build());

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Callable<JoinAttemptResult> joinAttempt1 = buildJoinAttempt(chatRoom.getId(), joiner1.getId(), readyLatch, startLatch);
        Callable<JoinAttemptResult> joinAttempt2 = buildJoinAttempt(chatRoom.getId(), joiner2.getId(), readyLatch, startLatch);

        try {
            Future<JoinAttemptResult> future1 = executorService.submit(joinAttempt1);
            Future<JoinAttemptResult> future2 = executorService.submit(joinAttempt2);

            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();

            // when
            startLatch.countDown();

            List<JoinAttemptResult> results = new ArrayList<>();
            results.add(future1.get(10, TimeUnit.SECONDS));
            results.add(future2.get(10, TimeUnit.SECONDS));

            // then
            long successCount = results.stream().filter(JoinAttemptResult::success).count();
            long fullCount = results.stream()
                    .filter(result -> result.errorCode() == ErrorCode.CHAT_ROOM_FULL)
                    .count();

            assertThat(successCount).isEqualTo(1);
            assertThat(fullCount).isEqualTo(1);
            assertThat(chatRoomMemberRepository.countByChatRoom_IdAndActiveTrue(chatRoom.getId())).isEqualTo(5);
        } finally {
            executorService.shutdownNow();
        }
    }

    private Callable<JoinAttemptResult> buildJoinAttempt(
            Long roomId,
            Long memberId,
            CountDownLatch readyLatch,
            CountDownLatch startLatch
    ) {
        return () -> {
            readyLatch.countDown();
            startLatch.await(5, TimeUnit.SECONDS);

            Member member = memberRepository.findById(memberId).orElseThrow();
            try {
                chatService.joinChatRoom(roomId, member);
                return new JoinAttemptResult(true, null);
            } catch (ChatException e) {
                return new JoinAttemptResult(false, e.getErrorCode());
            }
        };
    }

    private record JoinAttemptResult(boolean success, ErrorCode errorCode) {
    }
}
