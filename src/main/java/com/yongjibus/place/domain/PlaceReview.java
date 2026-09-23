package com.yongjibus.place.domain;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_place_review_member", columnNames = {"place_id", "member_id"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 120)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Member approvedBy;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime approvedAt;

    public static PlaceReview pending(Place place, Member member, int rating, String comment) {
        PlaceReview review = new PlaceReview();
        review.place = place;
        review.member = member;
        review.status = ReviewStatus.PENDING;
        review.updateContent(rating, comment);
        return review;
    }

    public void updateByMember(int rating, String comment) {
        if (status == ReviewStatus.HIDDEN) {
            throw new PlaceException(ErrorCode.INVALID_REVIEW_STATUS_TRANSITION);
        }
        updateContent(rating, comment);
        status = ReviewStatus.PENDING;
        clearApproval();
    }

    public void approve(Member operator) {
        if (status != ReviewStatus.PENDING) {
            throw new PlaceException(ErrorCode.INVALID_REVIEW_STATUS_TRANSITION);
        }
        place.requireApproved();
        markApproved(operator);
    }

    public void reject() {
        if (status != ReviewStatus.PENDING) {
            throw new PlaceException(ErrorCode.INVALID_REVIEW_STATUS_TRANSITION);
        }
        status = ReviewStatus.REJECTED;
        clearApproval();
    }

    public void hide() {
        if (status != ReviewStatus.APPROVED) {
            throw new PlaceException(ErrorCode.INVALID_REVIEW_STATUS_TRANSITION);
        }
        status = ReviewStatus.HIDDEN;
        clearApproval();
    }

    public void restore(Member operator) {
        if (status != ReviewStatus.HIDDEN) {
            throw new PlaceException(ErrorCode.INVALID_REVIEW_STATUS_TRANSITION);
        }
        place.requireApproved();
        markApproved(operator);
    }

    private void updateContent(int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new PlaceException(ErrorCode.INVALID_RATING);
        }
        if (comment == null || comment.trim().isEmpty() || comment.trim().length() > 120) {
            throw new PlaceException(ErrorCode.INVALID_REQUEST);
        }
        this.rating = rating;
        this.comment = comment.trim();
    }

    private void markApproved(Member operator) {
        status = ReviewStatus.APPROVED;
        approvedBy = operator;
        approvedAt = LocalDateTime.now();
    }

    private void clearApproval() {
        approvedBy = null;
        approvedAt = null;
    }
}
