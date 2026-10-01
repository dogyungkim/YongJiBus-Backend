package com.yongjibus.member.controller.dto;

import com.yongjibus.member.domain.Member;
import com.yongjibus.member.domain.MemberRole;

public record MemberResponseDTO(
    Long id,
    String email,
    String username,
    String name,
    MemberRole role
) {
    public static MemberResponseDTO from(Member member) {
        return new MemberResponseDTO(
                member.getId(),
                member.getEmail(),
                member.getUsername(),
                member.getName(),
                member.getRole()
        );
    }
}
