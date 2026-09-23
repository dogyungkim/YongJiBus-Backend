package com.yongjibus.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.PlaceException;
import com.yongjibus.place.client.KakaoLocalClient.KakaoPlace;

class PlaceSelectionProofServiceTest {
    private final PlaceSelectionProofService service =
            new PlaceSelectionProofService("test-nearby-selection-proof-secret-1234567890");

    @Test
    void proofIsBoundToMemberAndRejectsTampering() {
        KakaoPlace place = new KakaoPlace("123", "장소", "음식점", "도로명 주소", "지번 주소",
                "37.2242", "127.18766", "https://place.map.kakao.com/123");
        String proof = service.issue(1L, place);

        assertThat(service.verify(proof, 1L).placeId()).isEqualTo("123");
        assertInvalid(() -> service.verify(proof, 2L));
        assertInvalid(() -> service.verify(proof + "x", 1L));
    }

    private static void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(PlaceException.class)
                .extracting(error -> ((PlaceException) error).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_KAKAO_PLACE);
    }
}
