package com.yongjibus.place.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yongjibus.place.domain.PlaceImage;

public interface PlaceImageRepository extends JpaRepository<PlaceImage, Long> {
    List<PlaceImage> findByPlace_IdOrderBySortOrderAscIdAsc(Long placeId);

    List<PlaceImage> findByPlace_IdInOrderByPlace_IdAscSortOrderAscIdAsc(Collection<Long> placeIds);

    Optional<PlaceImage> findByIdAndPlace_Id(Long imageId, Long placeId);
}
