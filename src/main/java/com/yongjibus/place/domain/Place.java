package com.yongjibus.place.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.PlaceException;
import com.yongjibus.member.domain.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, length = 255)
    private String addressText;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 7)
    private BigDecimal longitude;

    @Column(nullable = false, length = 25)
    private String jusoBuildingManagementNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlaceCategory category;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PlaceSubcategory subcategory;

    @Column(nullable = false, unique = true, length = 32)
    private String kakaoPlaceId;

    @Column(nullable = false, length = 255)
    private String kakaoPlaceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlaceStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Member createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Member approvedBy;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime approvedAt;

    public static Place pending(String displayName, String addressText, BigDecimal latitude, BigDecimal longitude,
            String buildingNumber, PlaceCategory category, PlaceSubcategory subcategory,
            String kakaoPlaceId, String kakaoPlaceUrl, Member member) {
        return new Place(displayName, addressText, latitude, longitude, buildingNumber, category, subcategory,
                kakaoPlaceId, kakaoPlaceUrl, PlaceStatus.PENDING, member, null);
    }

    public static Place approved(String displayName, String addressText, BigDecimal latitude, BigDecimal longitude,
            String buildingNumber, PlaceCategory category, PlaceSubcategory subcategory,
            String kakaoPlaceId, String kakaoPlaceUrl, Member operator) {
        return new Place(displayName, addressText, latitude, longitude, buildingNumber, category, subcategory,
                kakaoPlaceId, kakaoPlaceUrl, PlaceStatus.APPROVED, operator, operator);
    }

    private Place(String displayName, String addressText, BigDecimal latitude, BigDecimal longitude,
            String buildingNumber, PlaceCategory category, PlaceSubcategory subcategory,
            String kakaoPlaceId, String kakaoPlaceUrl, PlaceStatus status, Member createdBy, Member approvedBy) {
        this.displayName = requireText(displayName, ErrorCode.INVALID_REQUEST);
        this.addressText = requireText(addressText, ErrorCode.PUBLIC_ADDRESS_INVALID);
        this.latitude = latitude;
        this.longitude = longitude;
        this.jusoBuildingManagementNumber = requireText(buildingNumber, ErrorCode.PUBLIC_ADDRESS_INVALID);
        changeCategory(category, subcategory);
        this.kakaoPlaceId = requireText(kakaoPlaceId, ErrorCode.INVALID_KAKAO_PLACE);
        this.kakaoPlaceUrl = requireText(kakaoPlaceUrl, ErrorCode.INVALID_KAKAO_PLACE);
        this.status = status;
        this.createdBy = createdBy;
        this.approvedBy = approvedBy;
        this.approvedAt = status == PlaceStatus.APPROVED ? LocalDateTime.now() : null;
    }

    public void approve(Member operator, String displayName, PlaceCategory category, PlaceSubcategory subcategory) {
        if (status != PlaceStatus.PENDING && status != PlaceStatus.REJECTED) {
            throw new PlaceException(ErrorCode.INVALID_PLACE_STATUS_TRANSITION);
        }
        this.displayName = requireText(displayName, ErrorCode.INVALID_REQUEST);
        changeCategory(category, subcategory);
        markApproved(operator);
    }

    public void reject() {
        if (status != PlaceStatus.PENDING) {
            throw new PlaceException(ErrorCode.INVALID_PLACE_STATUS_TRANSITION);
        }
        status = PlaceStatus.REJECTED;
        clearApproval();
    }

    public void hide() {
        if (status != PlaceStatus.APPROVED) {
            throw new PlaceException(ErrorCode.INVALID_PLACE_STATUS_TRANSITION);
        }
        status = PlaceStatus.HIDDEN;
        clearApproval();
    }

    public void restore(Member operator) {
        if (status != PlaceStatus.HIDDEN) {
            throw new PlaceException(ErrorCode.INVALID_PLACE_STATUS_TRANSITION);
        }
        markApproved(operator);
    }

    public void requireApproved() {
        if (status != PlaceStatus.APPROVED) {
            throw new PlaceException(ErrorCode.PLACE_NOT_APPROVED);
        }
    }

    private void changeCategory(PlaceCategory category, PlaceSubcategory subcategory) {
        if (category == null || (category == PlaceCategory.FOOD) != (subcategory != null)) {
            throw new PlaceException(ErrorCode.INVALID_PLACE_CATEGORY);
        }
        this.category = category;
        this.subcategory = subcategory;
    }

    private void markApproved(Member operator) {
        status = PlaceStatus.APPROVED;
        approvedBy = operator;
        approvedAt = LocalDateTime.now();
    }

    private void clearApproval() {
        approvedBy = null;
        approvedAt = null;
    }

    private static String requireText(String value, ErrorCode errorCode) {
        if (value == null || value.trim().isEmpty()) {
            throw new PlaceException(errorCode);
        }
        return value.trim();
    }
}
