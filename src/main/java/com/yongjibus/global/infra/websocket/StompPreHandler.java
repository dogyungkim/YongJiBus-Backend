package com.yongjibus.global.infra.websocket;

import java.security.Principal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import com.yongjibus.auth.domain.MemberDetail;
import com.yongjibus.auth.service.MemberDetailService;
import com.yongjibus.chat.domain.ChatRoom;
import com.yongjibus.chat.repository.ChatRoomMemberRepository;
import com.yongjibus.chat.repository.ChatRoomRepository;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.AuthException;
import com.yongjibus.global.error.exception.StompException;
import com.yongjibus.global.infra.jwt.JwtService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class StompPreHandler implements ChannelInterceptor {

    private static final Pattern CHAT_ROOM_DESTINATION_PATTERN = Pattern.compile("^/sub/chat/room/(\\d+)$");

    private final JwtService jwtService;
    private final MemberDetailService memberDetailService;
    private final WebsocketSessionManager websocketSessionManager;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        } else if (StompCommand.SEND.equals(accessor.getCommand())) {
            extractAuthenticatedMember(accessor);
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            websocketSessionManager.removeSessionBySessionId(accessor.getSessionId());
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authHeader = extractAuthorizationHeader(accessor)
            .orElseThrow(() -> new StompException(ErrorCode.UNAUTHORIZED));

        if (!authHeader.startsWith("Bearer ")) {
            throw new StompException(ErrorCode.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        if (!jwtService.validateAccessToken(token)) {
            throw new StompException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        String email = jwtService.getEmailFromToken(token);
        UserDetails userDetails = loadActiveUserDetails(email);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities()
        );

        accessor.setUser(authentication);
        websocketSessionManager.addSession(accessor.getSessionId(), email);
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Matcher matcher = CHAT_ROOM_DESTINATION_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return;
        }

        Long roomId = Long.valueOf(matcher.group(1));
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new StompException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        MemberDetail memberDetail = extractAuthenticatedMember(accessor);

        boolean isRoomMember = chatRoomMemberRepository
            .findByMemberAndChatRoomAndActiveTrue(memberDetail.getMember(), chatRoom)
            .isPresent();

        if (!isRoomMember) {
            throw new StompException(ErrorCode.CHAT_ROOM_FORBIDDEN);
        }
    }

    private MemberDetail extractAuthenticatedMember(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (!(user instanceof Authentication authentication)
                || !(authentication.getPrincipal() instanceof MemberDetail memberDetail)) {
            throw new StompException(ErrorCode.UNAUTHORIZED);
        }
        return (MemberDetail) loadActiveUserDetails(memberDetail.getMember().getEmail());
    }

    private Optional<String> extractAuthorizationHeader(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null) {
            authHeader = accessor.getFirstNativeHeader("authorization");
        }
        return Optional.ofNullable(authHeader);
    }

    private UserDetails loadActiveUserDetails(String email) {
        try {
            return memberDetailService.loadUserByUsername(email);
        } catch (AuthException e) {
            throw new StompException(e.getErrorCode());
        }
    }
}
