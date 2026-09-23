package com.yongjibus.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.yongjibus.member.domain.Member;
import com.yongjibus.place.client.JusoAddressClient;
import com.yongjibus.place.client.KakaoLocalClient;
import com.yongjibus.place.client.KakaoLocalClient.KakaoPlace;
import com.yongjibus.place.domain.Place;
import com.yongjibus.place.domain.PlaceCategory;
import com.yongjibus.place.repository.PlaceRepository;
import com.yongjibus.place.repository.PlaceReviewRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class PlaceServiceTest {
    @Test
    void rejectedAndHiddenPlacesAreBothExposedAsUnavailable() {
        PlaceRepository places = mock(PlaceRepository.class);
        KakaoLocalClient kakao = mock(KakaoLocalClient.class);
        PlaceSelectionProofService proofs = mock(PlaceSelectionProofService.class);
        PlaceService service = new PlaceService(places, mock(PlaceReviewRepository.class), kakao,
                mock(JusoAddressClient.class), proofs, mock(PlaceImageService.class), null,
                new SimpleMeterRegistry());
        Member member = Member.builder().id(10L).username("user").build();
        ReflectionTestUtils.setField(member, "id", 10L);
        Place rejected = place("1");
        rejected.reject();
        Place hidden = place("2");
        hidden.approve(member, "장소", PlaceCategory.CAFE, null);
        hidden.hide();
        ReflectionTestUtils.setField(rejected, "id", 101L);
        ReflectionTestUtils.setField(hidden, "id", 102L);
        List<KakaoPlace> results = List.of(kakao("1"), kakao("2"));
        when(kakao.search("장소")).thenReturn(results);
        when(places.findByKakaoPlaceIdIn(any())).thenReturn(List.of(rejected, hidden));
        when(proofs.issue(any(), any())).thenReturn("proof");

        var response = service.searchKakao(member, " 장소 ");

        assertThat(response).extracting(item -> item.registrationStatus())
                .containsExactly("UNAVAILABLE", "UNAVAILABLE");
        assertThat(response).extracting(item -> item.registeredPlaceId())
                .containsExactly(101L, 102L);
    }

    private static Place place(String kakaoId) {
        return Place.pending("장소", "주소", BigDecimal.valueOf(37.2242), BigDecimal.valueOf(127.18766),
                "1234567890123456789012345", PlaceCategory.CAFE, null, kakaoId,
                "https://place.map.kakao.com/" + kakaoId, Member.builder().id(1L).username("maker").build());
    }

    private static KakaoPlace kakao(String id) {
        return new KakaoPlace(id, "장소", "카페", "도로명", "지번", "37.2242", "127.18766",
                "https://place.map.kakao.com/" + id);
    }
}
