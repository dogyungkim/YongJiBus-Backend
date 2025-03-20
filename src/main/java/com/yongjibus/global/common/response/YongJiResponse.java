package com.yongjibus.global.common.response;

import org.springframework.http.ResponseEntity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class YongJiResponse<T> {
    private final int status;    // HTTP 상태 코드
    private final T data;         // 응답 데이터 (제네릭)

    // 성공 응답
    public static <T> ResponseEntity<YongJiResponse<T>> success(T data) {
        return ResponseEntity.ok(new YongJiResponse<>(200, data));
    }

    // 실패 응답
    public static ResponseEntity<YongJiResponse<String>> error(int code, String message) {
        return ResponseEntity.badRequest()
                .body(new YongJiResponse<>(code, message));
    }
}
