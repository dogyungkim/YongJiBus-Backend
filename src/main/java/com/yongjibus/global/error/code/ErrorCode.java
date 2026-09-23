package com.yongjibus.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {
    //Common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    // 인증 관련 오류
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 필요합니다."),
    INVALID_AUTH_CODE(HttpStatus.BAD_REQUEST, "유효하지 않은 인증 코드입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 사용자명입니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 전송에 실패했습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    MEMBER_DELETED(HttpStatus.UNAUTHORIZED, "회원 정보가 없습니다."),
    REPORT_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "신고 대상 사용자를 찾을 수 없습니다."),
    
    // 날짜 관련 오류
    DATE_INFO_NOT_FOUND(HttpStatus.NOT_FOUND, "날짜 정보를 찾을 수 없습니다."),
    INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "날짜 형식이 올바르지 않습니다."),
    INVALID_VACATION_PERIOD(HttpStatus.BAD_REQUEST, "방학 시작일은 종료일보다 늦을 수 없습니다."),
    OVERLAPPING_VACATION_PERIOD(HttpStatus.CONFLICT, "기존 방학 기간과 겹칠 수 없습니다."),

    // 시간표 관련 오류
    TIMETABLE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "시간표를 사용할 수 없습니다."),
    INVALID_TIMETABLE_RELEASE(HttpStatus.INTERNAL_SERVER_ERROR, "시간표 데이터를 처리할 수 없습니다."),

    // 토큰 관련 오류
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."), 
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_FORBIDDEN(HttpStatus.FORBIDDEN, "채팅방 참여자가 아닙니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증된 사용자가 아닙니다."),
    FCM_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "FCM 토큰을 찾을 수 없습니다."),
    
    // 웹소켓 오류
    // WebsocketStompErrorHandler 에서 사용

    // 채팅 오류
    CHAT_ROOM_FULL(HttpStatus.BAD_REQUEST, "채팅방 인원이 가득 찼습니다."),
    CHAT_MESSAGE_SAVE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "메시지 저장 중 오류 발생"),

    // 장소 및 평가 오류
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다."),
    PLACE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 장소입니다."),
    PLACE_REQUEST_CONFLICT(HttpStatus.CONFLICT, "장소 등록 요청이 충돌했습니다. 다시 시도해 주세요."),
    PLACE_NOT_APPROVED(HttpStatus.BAD_REQUEST, "승인된 장소가 아닙니다."),
    PLACE_REQUEST_NOT_ALLOWED(HttpStatus.CONFLICT, "현재 상태에서는 장소 등록을 요청할 수 없습니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "평가를 찾을 수 없습니다."),
    INVALID_RATING(HttpStatus.BAD_REQUEST, "별점은 1점부터 5점까지 입력해야 합니다."),
    INVALID_PLACE_CATEGORY(HttpStatus.BAD_REQUEST, "장소 카테고리와 하위 카테고리가 올바르지 않습니다."),
    INVALID_KAKAO_PLACE(HttpStatus.BAD_REQUEST, "유효하지 않은 카카오 장소 정보입니다."),
    PLACE_SEARCH_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "장소 검색 서비스를 사용할 수 없습니다."),
    PLACE_SEARCH_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "장소 검색 요청이 너무 많습니다."),
    PUBLIC_ADDRESS_INVALID(HttpStatus.BAD_REQUEST, "공공주소 형식이 올바르지 않습니다."),
    PUBLIC_ADDRESS_NOT_RESOLVED(HttpStatus.UNPROCESSABLE_ENTITY, "주소를 정확히 확인할 수 없습니다."),
    PUBLIC_ADDRESS_SEARCH_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "공공주소 검색 서비스를 사용할 수 없습니다."),
    PUBLIC_ADDRESS_RESOLVE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "공공주소 좌표 서비스를 사용할 수 없습니다."),
    OPERATOR_REQUIRED(HttpStatus.FORBIDDEN, "운영자 권한이 필요합니다."),
    INVALID_PLACE_STATUS_TRANSITION(HttpStatus.CONFLICT, "장소 상태를 변경할 수 없습니다."),
    INVALID_REVIEW_STATUS_TRANSITION(HttpStatus.CONFLICT, "평가 상태를 변경할 수 없습니다."),
    INVALID_PLACE_IMAGE(HttpStatus.BAD_REQUEST, "JPEG, PNG, WEBP 형식의 5MB 이하 이미지만 등록할 수 있습니다."),
    TOO_MANY_PLACE_IMAGES(HttpStatus.BAD_REQUEST, "장소 이미지는 최대 5개까지 등록할 수 있습니다."),
    PLACE_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "장소 이미지를 찾을 수 없습니다."),
    PLACE_IMAGE_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "장소 이미지 저장에 실패했습니다."),
    ;

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

}
