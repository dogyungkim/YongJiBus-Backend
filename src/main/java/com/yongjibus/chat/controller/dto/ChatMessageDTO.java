package com.yongjibus.chat.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatMessageDTO(
    @NotBlank(message = "메시지 내용을 입력해주세요.")
    String content,

    @NotNull(message = "채팅방 ID가 필요합니다.")
    Long roomId
) {
}
