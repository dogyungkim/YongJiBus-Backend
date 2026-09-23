package com.yongjibus.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.yongjibus.member.domain.Member;
import com.yongjibus.member.repository.MemberRepository;
import com.yongjibus.place.client.JusoAddressClient;
import com.yongjibus.place.client.JusoAddressClient.ResolvedAddress;
import com.yongjibus.place.client.KakaoLocalClient.KakaoPlace;
import com.yongjibus.place.controller.dto.PlaceDTOs.PlaceRequest;
import com.yongjibus.place.domain.PlaceCategory;
import com.yongjibus.place.domain.ReviewStatus;
import com.yongjibus.place.repository.PlaceRepository;
import com.yongjibus.place.repository.PlaceReviewRepository;

@SpringBootTest
@ActiveProfiles("test")
class PlaceRequestIntegrationTest {
    @Autowired
    PlaceService placeService;
    @Autowired
    PlaceSelectionProofService proofService;
    @Autowired
    PlaceRepository placeRepository;
    @Autowired
    PlaceReviewRepository reviewRepository;
    @Autowired
    MemberRepository memberRepository;
    @MockBean
    JusoAddressClient jusoAddressClient;

    @AfterEach
    void cleanUp() {
        reviewRepository.deleteAll();
        placeRepository.deleteAll();
        memberRepository.findByUsername("placeuser").ifPresent(memberRepository::delete);
    }

    @Test
    void newRequestCreatesOnePlaceAndUpsertsTheMembersReview() {
        Member member = memberRepository.save(Member.builder().name("요청자").username("placeuser")
                .email("placeuser@m.ji").password("password").build());
        KakaoPlace kakao = new KakaoPlace("987654", "카페", "카페", "도로명 주소", "지번 주소",
                "37.2242", "127.18766", "https://place.map.kakao.com/987654");
        String proof = proofService.issue(member.getId(), kakao);
        ResolvedAddress address = new ResolvedAddress("공공 도로명 주소", "1234567890123456789012345",
                new BigDecimal("37.2242000"), new BigDecimal("127.1876600"));
        when(jusoAddressClient.resolve("도로명 주소", "지번 주소")).thenReturn(address);

        var first = placeService.requestPlace(member,
                new PlaceRequest(proof, "첫 이름", PlaceCategory.CAFE, null, 5, "첫 평가"));
        var second = placeService.requestPlace(member,
                new PlaceRequest(proof, "바뀐 이름", PlaceCategory.CAFE, null, 3, "수정 평가"));

        assertThat(first.placeId()).isEqualTo(second.placeId());
        assertThat(placeRepository.count()).isEqualTo(1);
        assertThat(reviewRepository.count()).isEqualTo(1);
        assertThat(reviewRepository.findByPlaceIdAndMemberId(first.placeId(), member.getId()))
                .get()
                .satisfies(review -> {
                    assertThat(review.getRating()).isEqualTo(3);
                    assertThat(review.getComment()).isEqualTo("수정 평가");
                    assertThat(review.getStatus()).isEqualTo(ReviewStatus.PENDING);
                });
        verify(jusoAddressClient, times(1)).resolve("도로명 주소", "지번 주소");
    }
}
