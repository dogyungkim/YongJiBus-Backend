package com.yongjibus.member.controller.dto;

import com.yongjibus.member.domain.Member;

public record MemberResponseDTO(
    Long id,
    String email,
    String username,
    String name
) {
    public static MemberResponseDTO from(Member member) {
        return new MemberResponseDTO(
                member.getId(),
                member.getEmail(),
                member.getUsername(),
                member.getName()
        );
    }
}