package com.yongjibus.place.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yongjibus.place.domain.Place;
import com.yongjibus.place.domain.PlaceStatus;

import jakarta.persistence.LockModeType;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    Optional<Place> findByKakaoPlaceId(String kakaoPlaceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Place p where p.id = :placeId")
    Optional<Place> findByIdForUpdate(@Param("placeId") Long placeId);

    List<Place> findByKakaoPlaceIdIn(Collection<String> kakaoPlaceIds);

    Slice<Place> findByStatusOrderByCreatedAtDescIdDesc(PlaceStatus status, Pageable pageable);

    @Query(value = """
            SELECT p.id AS id, p.display_name AS displayName, p.address_text AS addressText,
                   p.latitude AS latitude, p.longitude AS longitude, p.category AS category,
                   p.subcategory AS subcategory, AVG(r.rating) AS averageRating,
                   COUNT(r.id) AS reviewCount, latest.comment AS representativeReview,
                   p.kakao_place_url AS kakaoPlaceUrl
              FROM place p
              LEFT JOIN place_review r ON r.place_id = p.id AND r.status = 'APPROVED'
              LEFT JOIN (
                   SELECT ranked.place_id, ranked.comment
                     FROM (
                          SELECT pr.place_id, pr.comment,
                                 ROW_NUMBER() OVER (PARTITION BY pr.place_id
                                     ORDER BY pr.approved_at DESC, pr.id DESC) AS row_num
                            FROM place_review pr
                           WHERE pr.status = 'APPROVED'
                     ) ranked
                    WHERE ranked.row_num = 1
              ) latest ON latest.place_id = p.id
             WHERE p.status = 'APPROVED'
             GROUP BY p.id, p.display_name, p.address_text, p.latitude, p.longitude,
                      p.category, p.subcategory, latest.comment, p.kakao_place_url
             ORDER BY p.id
            """, nativeQuery = true)
    List<PlaceSummaryProjection> findApprovedSummaries();

    interface PlaceSummaryProjection {
        Long getId();

        String getDisplayName();

        String getAddressText();

        BigDecimal getLatitude();

        BigDecimal getLongitude();

        String getCategory();

        String getSubcategory();

        Double getAverageRating();

        long getReviewCount();

        String getRepresentativeReview();

        String getKakaoPlaceUrl();
    }
}
