package com.yongjibus.place.repository;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yongjibus.place.domain.PlaceReview;
import com.yongjibus.place.domain.ReviewStatus;

public interface PlaceReviewRepository extends JpaRepository<PlaceReview, Long> {
    Optional<PlaceReview> findByPlaceIdAndMemberId(Long placeId, Long memberId);

    Slice<PlaceReview> findByPlaceIdAndStatusOrderByCreatedAtDescIdDesc(
            Long placeId, ReviewStatus status, Pageable pageable);

    Slice<PlaceReview> findByStatusOrderByCreatedAtDescIdDesc(ReviewStatus status, Pageable pageable);

    @Modifying(flushAutomatically = true)
    @Query("""
            update PlaceReview r
               set r.status = com.yongjibus.place.domain.ReviewStatus.REJECTED,
                   r.approvedBy = null,
                   r.approvedAt = null,
                   r.updatedAt = CURRENT_TIMESTAMP
             where r.place.id = :placeId
               and r.status = com.yongjibus.place.domain.ReviewStatus.PENDING
            """)
    int rejectPendingByPlaceId(@Param("placeId") Long placeId);
}
