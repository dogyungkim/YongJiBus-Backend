package com.yongjibus.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.yongjibus.chat.domain.ChatRoom;
import com.yongjibus.chat.domain.ChatRoomMember;
import com.yongjibus.member.domain.Member;

@DataJpaTest
class ChatRoomMemberRepositoryTest {

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("활성 멤버십이 있는 채팅방만 내 채팅방 목록으로 조회한다")
    void findActiveChatRoomsByMember_ShouldReturnOnlyActiveRooms() {
        // given
        Member member = persistMember("member@mju.ac.kr", "member");
        ChatRoom activeRoom = persistRoom("활성 채팅방", LocalTime.of(8, 0));
        ChatRoom inactiveRoom = persistRoom("나간 채팅방", LocalTime.of(9, 0));

        entityManager.persist(ChatRoomMember.builder()
                .member(member)
                .chatRoom(activeRoom)
                .joinedAt(LocalDateTime.of(2026, 3, 30, 8, 0))
                .active(true)
                .build());
        entityManager.persist(ChatRoomMember.builder()
                .member(member)
                .chatRoom(inactiveRoom)
                .joinedAt(LocalDateTime.of(2026, 3, 30, 9, 0))
                .active(false)
                .build());
        entityManager.flush();
        entityManager.clear();

        // when
        List<ChatRoom> result = chatRoomMemberRepository.findActiveChatRoomsByMember(member);

        // then
        assertThat(result)
                .extracting(ChatRoom::getName)
                .containsExactly("활성 채팅방");
    }

    @Test
    @DisplayName("현재 활성 참여 중이 아닌 채팅방은 미참여 목록에 포함된다")
    void findChatRoomsNotJoinedByMember_ShouldIncludeInactiveOrNeverJoinedRooms() {
        // given
        Member member = persistMember("member2@mju.ac.kr", "member2");
        ChatRoom activeRoom = persistRoom("참여 중 채팅방", LocalTime.of(8, 0));
        ChatRoom inactiveRoom = persistRoom("예전에 나간 채팅방", LocalTime.of(9, 0));
        ChatRoom neverJoinedRoom = persistRoom("한 번도 안 들어간 채팅방", LocalTime.of(10, 0));

        entityManager.persist(ChatRoomMember.builder()
                .member(member)
                .chatRoom(activeRoom)
                .joinedAt(LocalDateTime.of(2026, 3, 30, 8, 0))
                .active(true)
                .build());
        entityManager.persist(ChatRoomMember.builder()
                .member(member)
                .chatRoom(inactiveRoom)
                .joinedAt(LocalDateTime.of(2026, 3, 30, 9, 0))
                .active(false)
                .build());
        entityManager.flush();
        entityManager.clear();

        // when
        List<ChatRoom> result = chatRoomMemberRepository.findChatRoomsNotJoinedByMember(member);

        // then
        assertThat(result)
                .extracting(ChatRoom::getName)
                .containsExactlyInAnyOrder("예전에 나간 채팅방", "한 번도 안 들어간 채팅방");
        assertThat(result)
                .extracting(ChatRoom::getName)
                .doesNotContain("참여 중 채팅방");
    }

    @Test
    @DisplayName("활성 멤버십의 joinedAt만 조회한다")
    void findJoinedAtByMemberAndChatRoom_ShouldReturnJoinedAtForActiveMembership() {
        // given
        Member member = persistMember("member3@mju.ac.kr", "member3");
        ChatRoom room = persistRoom("joined-at-room", LocalTime.of(11, 0));
        LocalDateTime joinedAt = LocalDateTime.of(2026, 3, 30, 11, 30);

        entityManager.persist(ChatRoomMember.builder()
                .member(member)
                .chatRoom(room)
                .joinedAt(joinedAt)
                .active(true)
                .build());
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<LocalDateTime> result = chatRoomMemberRepository.findJoinedAtByMemberAndChatRoom(member, room);

        // then
        assertThat(result).contains(joinedAt);
    }

    private Member persistMember(String email, String username) {
        Member member = Member.builder()
                .name("테스터")
                .username(username)
                .email(email)
                .password("password123")
                .build();
        return entityManager.persistAndFlush(member);
    }

    private ChatRoom persistRoom(String name, LocalTime departureTime) {
        return entityManager.persistAndFlush(ChatRoom.builder()
                .name(name)
                .departureTime(departureTime)
                .build());
    }
}
