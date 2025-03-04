package com.yongjibus.member.controller.dto;

import com.yongjibus.auth.domain.Member;

import lombok.Builder;

@Builder
public record MemberResponseDTO(
    Long id,
    String email,
    String username,
    String name
) {
    public static MemberResponseDTO from(Member member) {
        return MemberResponseDTO.builder()
                .id(member.getId())
                .email(member.getEmail())
                .username(member.getUsername())
                .name(member.getName())
                .build();
    }
}