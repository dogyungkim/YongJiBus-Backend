package com.yongjibus.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.yongjibus.member.domain.Member;
import com.yongjibus.place.client.JusoAddressClient;
import com.yongjibus.place.client.KakaoLocalClient;
import com.yongjibus.place.client.KakaoLocalClient.KakaoPlace;
import com.yongjibus.place.domain.Place;
import com.yongjibus.place.domain.PlaceCategory;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.PlaceException;
import com.yongjibus.place.repository.PlaceRepository;
import com.yongjibus.place.repository.PlaceReviewRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class PlaceServiceTest {
    private static final double CAMPUS_LATITUDE = 37.2242;
    private static final double CAMPUS_LONGITUDE = 127.18766;

    @Test
    void rejectedAndHiddenPlacesAreBothExposedAsUnavailable() {
        PlaceRepository places = mock(PlaceRepository.class);
        KakaoLocalClient kakao = mock(KakaoLocalClient.class);
        PlaceSelectionProofService proofs = mock(PlaceSelectionProofService.class);
        PlaceService service = new PlaceService(places, mock(PlaceReviewRepository.class), kakao,
                mock(JusoAddressClient.class), proofs, mock(PlaceImageService.class), null,
                new SimpleMeterRegistry());
        Member member = member();
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

    @Test
    void viewportSearchFiltersOutOfBoundsPlacesDeduplicatesAndIssuesSelectionProofs() {
        PlaceRepository places = mock(PlaceRepository.class);
        KakaoLocalClient kakao = mock(KakaoLocalClient.class);
        PlaceSelectionProofService proofs = mock(PlaceSelectionProofService.class);
        PlaceService service = service(places, kakao, proofs, new SimpleMeterRegistry());
        Member member = member();
        double minLatitude = 37.223;
        double minLongitude = 127.186;
        double maxLatitude = 37.225;
        double maxLongitude = 127.189;
        KakaoPlace near = kakao("2", 37.2241, 127.1875);
        KakaoPlace duplicate = kakao("2", 37.2248, 127.1875);
        KakaoPlace far = kakao("1", 37.2249, 127.188);
        KakaoPlace outsideViewport = kakao("3", 37.226, 127.188);
        KakaoPlace outsideCampus = kakao("4", 37.28, 127.188);
        when(kakao.isViewportWithinSearchArea(minLatitude, minLongitude, maxLatitude, maxLongitude)).thenReturn(true);
        when(kakao.isWithinCampusArea(anyDouble(), anyDouble())).thenAnswer(invocation ->
                KakaoLocalClient.distanceMeters(CAMPUS_LATITUDE, CAMPUS_LONGITUDE,
                        invocation.getArgument(0), invocation.getArgument(1)) <= 5000);
        when(kakao.searchViewport(minLatitude, minLongitude, maxLatitude, maxLongitude))
                .thenReturn(List.of(far, near, duplicate, outsideViewport, outsideCampus));
        when(places.findByKakaoPlaceIdIn(any())).thenReturn(List.of());
        when(proofs.issue(any(), any())).thenAnswer(invocation ->
                "proof-" + ((KakaoPlace) invocation.getArgument(1)).id());

        var response = service.searchViewport(member, minLatitude, minLongitude, maxLatitude, maxLongitude);

        assertThat(response).extracting(item -> item.placeId()).containsExactly("2", "1");
        assertThat(response).extracting(item -> item.selectionProof()).containsExactly("proof-2", "proof-1");
        verify(proofs).issue(10L, near);
        verify(kakao).searchViewport(minLatitude, minLongitude, maxLatitude, maxLongitude);
    }

    @Test
    void viewportSearchRejectsInvalidBoundsAndCapsTheReturnedResults() {
        PlaceRepository places = mock(PlaceRepository.class);
        KakaoLocalClient kakao = mock(KakaoLocalClient.class);
        PlaceSelectionProofService proofs = mock(PlaceSelectionProofService.class);
        PlaceService service = service(places, kakao, proofs, new SimpleMeterRegistry());
        Member member = member();

        assertThatThrownBy(() -> service.searchViewport(member, 37.225, 127.186, 37.223, 127.189))
                .isInstanceOfSatisfying(PlaceException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        assertThatThrownBy(() -> service.searchViewport(member, 37.223, 127.186, 37.225, 127.189))
                .isInstanceOfSatisfying(PlaceException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        verify(kakao).isViewportWithinSearchArea(37.223, 127.186, 37.225, 127.189);

        List<KakaoPlace> results = IntStream.range(0, 31)
                .mapToObj(index -> kakao(String.format("%02d", index),
                        CAMPUS_LATITUDE + index * 0.00001, CAMPUS_LONGITUDE))
                .toList();
        when(kakao.isViewportWithinSearchArea(37.223, 127.186, 37.225, 127.189)).thenReturn(true);
        when(kakao.isWithinCampusArea(anyDouble(), anyDouble())).thenReturn(true);
        when(kakao.searchViewport(37.223, 127.186, 37.225, 127.189)).thenReturn(results);
        when(places.findByKakaoPlaceIdIn(any())).thenReturn(List.of());
        when(proofs.issue(any(), any())).thenReturn("proof");

        var response = service.searchViewport(member, 37.223, 127.186, 37.225, 127.189);

        assertThat(response).hasSize(30);
        assertThat(response).extracting(item -> item.placeId())
                .containsExactlyElementsOf(IntStream.range(0, 30).mapToObj(index -> String.format("%02d", index)).toList());
    }

    @Test
    void viewportSearchKeepsTheMemberRateLimit() {
        PlaceRepository places = mock(PlaceRepository.class);
        KakaoLocalClient kakao = mock(KakaoLocalClient.class);
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        PlaceService service = service(places, kakao, mock(PlaceSelectionProofService.class), meters);
        Member member = member();
        double minLatitude = 37.223;
        double minLongitude = 127.186;
        double maxLatitude = 37.225;
        double maxLongitude = 127.189;
        when(kakao.isViewportWithinSearchArea(minLatitude, minLongitude, maxLatitude, maxLongitude)).thenReturn(true);
        when(kakao.searchViewport(minLatitude, minLongitude, maxLatitude, maxLongitude)).thenReturn(List.of());

        for (int i = 0; i < 10; i++) {
            service.searchViewport(member, minLatitude, minLongitude, maxLatitude, maxLongitude);
        }
        assertThatThrownBy(() -> service.searchViewport(
                member, minLatitude, minLongitude, maxLatitude, maxLongitude))
                .isInstanceOfSatisfying(PlaceException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PLACE_SEARCH_RATE_LIMITED));

        assertThat(meters.get("nearby.kakao.search.success").counter().count()).isEqualTo(10);
        assertThat(meters.get("nearby.kakao.search.rate_limited").counter().count()).isEqualTo(1);
        verify(kakao, times(10)).searchViewport(minLatitude, minLongitude, maxLatitude, maxLongitude);
    }

    private static PlaceService service(PlaceRepository places, KakaoLocalClient kakao,
            PlaceSelectionProofService proofs, SimpleMeterRegistry meters) {
        return new PlaceService(places, mock(PlaceReviewRepository.class), kakao,
                mock(JusoAddressClient.class), proofs, mock(PlaceImageService.class), null, meters);
    }

    private static Member member() {
        Member member = Member.builder().id(10L).username("user").build();
        ReflectionTestUtils.setField(member, "id", 10L);
        return member;
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

    private static KakaoPlace kakao(String id, double latitude, double longitude) {
        return new KakaoPlace(id, "장소", "카페", "도로명", "지번", Double.toString(latitude),
                Double.toString(longitude), "https://place.map.kakao.com/" + id);
    }
}
