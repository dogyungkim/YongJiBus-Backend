package com.yongjibus.place.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.PlaceException;
import com.yongjibus.member.domain.Member;
import com.yongjibus.member.domain.MemberRole;

class PlaceDomainTest {
    private final Member user = member("user", MemberRole.USER);
    private final Member operator = member("operator", MemberRole.OPERATOR);

    @Test
    void categoryAndApprovalMetadataFollowTheStateRules() {
        assertThatThrownBy(() -> pending(PlaceCategory.FOOD, null))
                .isInstanceOf(PlaceException.class)
                .extracting(error -> ((PlaceException) error).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PLACE_CATEGORY);

        Place place = pending(PlaceCategory.FOOD, PlaceSubcategory.KOREAN);
        place.approve(operator, "확정 이름", PlaceCategory.CAFE, null);

        assertThat(place.getStatus()).isEqualTo(PlaceStatus.APPROVED);
        assertThat(place.getApprovedBy()).isEqualTo(operator);
        assertThat(place.getApprovedAt()).isNotNull();

        place.hide();
        assertThat(place.getStatus()).isEqualTo(PlaceStatus.HIDDEN);
        assertThat(place.getApprovedBy()).isNull();
        assertThat(place.getApprovedAt()).isNull();
    }

    @Test
    void hiddenReviewCannotBeEditedByItsAuthor() {
        Place place = pending(PlaceCategory.CAFE, null);
        place.approve(operator, "카페", PlaceCategory.CAFE, null);
        PlaceReview review = PlaceReview.pending(place, user, 5, "좋아요");
        review.approve(operator);
        review.hide();

        assertThatThrownBy(() -> review.updateByMember(3, "수정"))
                .isInstanceOf(PlaceException.class)
                .extracting(error -> ((PlaceException) error).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REVIEW_STATUS_TRANSITION);
    }

    private Place pending(PlaceCategory category, PlaceSubcategory subcategory) {
        return Place.pending("장소", "경기도 용인시 처인구", BigDecimal.valueOf(37.2242),
                BigDecimal.valueOf(127.18766), "1234567890123456789012345", category, subcategory,
                "123", "https://place.map.kakao.com/123", user);
    }

    private static Member member(String username, MemberRole role) {
        return Member.builder().name("이름").username(username).email(username + "@mju.kr")
                .password("password").role(role).build();
    }
}
