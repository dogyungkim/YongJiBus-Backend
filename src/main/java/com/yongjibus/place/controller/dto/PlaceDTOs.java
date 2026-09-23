package com.yongjibus.place.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.math.RoundingMode;
import java.util.List;

import com.yongjibus.member.domain.Member;
import com.yongjibus.place.domain.Place;
import com.yongjibus.place.domain.PlaceCategory;
import com.yongjibus.place.domain.PlaceReview;
import com.yongjibus.place.domain.PlaceStatus;
import com.yongjibus.place.domain.PlaceSubcategory;
import com.yongjibus.place.domain.ReviewStatus;
import com.yongjibus.place.repository.PlaceRepository.PlaceSummaryProjection;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class PlaceDTOs {
    private PlaceDTOs() {
    }

    public record PlaceRequest(
            @NotBlank @Size(max = 2048) String selectionProof,
            @NotBlank @Size(max = 100) String displayName,
            @NotNull PlaceCategory category,
            PlaceSubcategory subcategory,
            @Min(1) @Max(5) int rating,
            @NotBlank @Size(max = 120) String comment) {
    }

    public record ReviewRequest(
            @Min(1) @Max(5) int rating,
            @NotBlank @Size(max = 120) String comment) {
    }

    public record AdminPlaceRequest(
            @NotBlank @Size(max = 2048) String selectionProof,
            @NotBlank @Size(max = 100) String displayName,
            @NotNull PlaceCategory category,
            PlaceSubcategory subcategory) {
    }

    public record ApprovePlaceRequest(
            @NotBlank @Size(max = 100) String displayName,
            @NotNull PlaceCategory category,
            PlaceSubcategory subcategory) {
    }

    public record PlaceSummary(
            Long id,
            String displayName,
            String addressText,
            BigDecimal latitude,
            BigDecimal longitude,
            PlaceCategory category,
            PlaceSubcategory subcategory,
            Double averageRating,
            long reviewCount,
            String representativeReview,
            String kakaoPlaceUrl,
            String thumbnailUrl) {
        public static PlaceSummary from(PlaceSummaryProjection value, String thumbnailUrl) {
            return new PlaceSummary(value.getId(), value.getDisplayName(), value.getAddressText(),
                    value.getLatitude(), value.getLongitude(), PlaceCategory.valueOf(value.getCategory()),
                    value.getSubcategory() == null ? null : PlaceSubcategory.valueOf(value.getSubcategory()),
                    value.getAverageRating() == null ? null : BigDecimal.valueOf(value.getAverageRating())
                            .setScale(1, RoundingMode.HALF_UP).doubleValue(),
                    value.getReviewCount(), value.getRepresentativeReview(),
                    value.getKakaoPlaceUrl(), thumbnailUrl);
        }
    }

    public record PlaceImageItem(Long id, String imageUrl, String thumbnailUrl, int sortOrder) {
        public PlaceImageItem(Long id, String imageUrl, int sortOrder) {
            this(id, imageUrl, imageUrl, sortOrder);
        }
    }

    public record MyReview(
            Long id,
            int rating,
            String comment,
            ReviewStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        public static MyReview from(PlaceReview review) {
            return new MyReview(review.getId(), review.getRating(), review.getComment(), review.getStatus(),
                    review.getCreatedAt(), review.getUpdatedAt());
        }
    }

    public record PlaceDetail(
            Long id,
            String displayName,
            String addressText,
            BigDecimal latitude,
            BigDecimal longitude,
            PlaceCategory category,
            PlaceSubcategory subcategory,
            Double averageRating,
            long reviewCount,
            String representativeReview,
            String kakaoPlaceUrl,
            String thumbnailUrl,
            List<String> imageUrls,
            MyReview myReview) {
        public static PlaceDetail of(PlaceSummary summary, List<String> imageUrls, MyReview myReview) {
            return new PlaceDetail(summary.id(), summary.displayName(), summary.addressText(), summary.latitude(),
                    summary.longitude(), summary.category(), summary.subcategory(), summary.averageRating(),
                    summary.reviewCount(), summary.representativeReview(), summary.kakaoPlaceUrl(),
                    summary.thumbnailUrl(), imageUrls, myReview);
        }
    }

    public record PlaceReviewItem(
            Long id,
            String username,
            int rating,
            String comment,
            boolean isMine,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        public static PlaceReviewItem from(PlaceReview review, Long memberId) {
            return new PlaceReviewItem(review.getId(), review.getMember().getUsername(), review.getRating(),
                    review.getComment(), memberId != null && memberId.equals(review.getMember().getId()),
                    review.getCreatedAt(), review.getUpdatedAt());
        }
    }

    public record KakaoSearchItem(
            String placeId,
            String placeName,
            String categoryName,
            String roadAddressName,
            String addressName,
            BigDecimal latitude,
            BigDecimal longitude,
            String placeUrl,
            Long registeredPlaceId,
            String registrationStatus,
            String selectionProof) {
    }

    public record PlaceRequestResult(Long placeId, PlaceStatus placeStatus, ReviewStatus reviewStatus) {
    }

    public record AdminMember(Long id, String username) {
        public static AdminMember from(Member member) {
            return member == null ? null : new AdminMember(member.getId(), member.getUsername());
        }
    }

    public record AdminPlace(
            Long id,
            String displayName,
            String addressText,
            BigDecimal latitude,
            BigDecimal longitude,
            String jusoBuildingManagementNumber,
            PlaceCategory category,
            PlaceSubcategory subcategory,
            String kakaoPlaceId,
            String kakaoPlaceUrl,
            PlaceStatus status,
            AdminMember createdBy,
            AdminMember approvedBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime approvedAt,
            List<PlaceImageItem> images) {
        public static AdminPlace from(Place place, List<PlaceImageItem> images) {
            return new AdminPlace(place.getId(), place.getDisplayName(), place.getAddressText(), place.getLatitude(),
                    place.getLongitude(), place.getJusoBuildingManagementNumber(), place.getCategory(),
                    place.getSubcategory(), place.getKakaoPlaceId(), place.getKakaoPlaceUrl(), place.getStatus(),
                    AdminMember.from(place.getCreatedBy()), AdminMember.from(place.getApprovedBy()),
                    place.getCreatedAt(), place.getUpdatedAt(), place.getApprovedAt(), images);
        }
    }

    public record AdminReview(
            Long id,
            Long placeId,
            String placeDisplayName,
            AdminMember member,
            int rating,
            String comment,
            ReviewStatus status,
            AdminMember approvedBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime approvedAt) {
        public static AdminReview from(PlaceReview review) {
            return new AdminReview(review.getId(), review.getPlace().getId(), review.getPlace().getDisplayName(),
                    AdminMember.from(review.getMember()), review.getRating(), review.getComment(), review.getStatus(),
                    AdminMember.from(review.getApprovedBy()), review.getCreatedAt(), review.getUpdatedAt(),
                    review.getApprovedAt());
        }
    }
}
