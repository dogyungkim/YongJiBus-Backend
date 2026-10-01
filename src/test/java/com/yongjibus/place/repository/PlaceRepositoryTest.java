package com.yongjibus.place.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.yongjibus.member.domain.Member;
import com.yongjibus.member.domain.MemberRole;
import com.yongjibus.member.repository.MemberRepository;
import com.yongjibus.place.domain.Place;
import com.yongjibus.place.domain.PlaceCategory;
import com.yongjibus.place.domain.PlaceReview;

@DataJpaTest
class PlaceRepositoryTest {
    @Autowired
    PlaceRepository placeRepository;
    @Autowired
    PlaceReviewRepository reviewRepository;
    @Autowired
    MemberRepository memberRepository;

    @Test
    void publicProjectionUsesOnlyApprovedPlacesAndReviews() {
        Member operator = memberRepository.save(member("operator", MemberRole.OPERATOR));
        Member first = memberRepository.save(member("first", MemberRole.USER));
        Member second = memberRepository.save(member("second", MemberRole.USER));
        Place approved = placeRepository.saveAndFlush(Place.approved("승인 장소", "주소", latitude(), longitude(),
                "1234567890123456789012345", PlaceCategory.CAFE, null, "1",
                "https://place.map.kakao.com/1", operator));
        placeRepository.saveAndFlush(Place.pending("대기 장소", "주소", latitude(), longitude(),
                "2234567890123456789012345", PlaceCategory.CAFE, null, "2",
                "https://place.map.kakao.com/2", first));
        PlaceReview firstReview = PlaceReview.pending(approved, first, 4, "첫 평가");
        firstReview.approve(operator);
        reviewRepository.saveAndFlush(firstReview);
        PlaceReview secondReview = PlaceReview.pending(approved, second, 5, "대표 평가");
        secondReview.approve(operator);
        reviewRepository.saveAndFlush(secondReview);
        reviewRepository.saveAndFlush(PlaceReview.pending(approved, operator, 1, "대기 평가"));

        var summaries = placeRepository.findApprovedSummaries();

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getAverageRating()).isEqualTo(4.5);
        assertThat(summaries.get(0).getReviewCount()).isEqualTo(2);
        assertThat(summaries.get(0).getRepresentativeReview()).isEqualTo("대표 평가");
    }

    @Test
    void findByIdForUpdateReturnsExistingPlace() {
        Member member = memberRepository.save(member("owner", MemberRole.USER));
        Place place = placeRepository.saveAndFlush(Place.pending("대기 장소", "주소", latitude(), longitude(),
                "1234567890123456789012345", PlaceCategory.CAFE, null, "1",
                "https://place.map.kakao.com/1", member));

        assertThat(placeRepository.findByIdForUpdate(place.getId())).containsSame(place);
    }

    private static Member member(String username, MemberRole role) {
        return Member.builder().name("이름").username(username).email(username + "@m.ji")
                .password("password").role(role).build();
    }

    private static BigDecimal latitude() {
        return new BigDecimal("37.2242000");
    }

    private static BigDecimal longitude() {
        return new BigDecimal("127.1876600");
    }
}
