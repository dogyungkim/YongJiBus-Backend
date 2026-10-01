package com.yongjibus.place.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_place_image_storage_key", columnNames = "storage_key"),
        @UniqueConstraint(name = "uk_place_image_thumbnail_storage_key", columnNames = "thumbnail_storage_key"),
        @UniqueConstraint(name = "uk_place_image_sort_order", columnNames = {"place_id", "sort_order"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(nullable = false, length = 255)
    private String storageKey;

    @Column(nullable = false, length = 255)
    private String thumbnailStorageKey;

    @Column(nullable = false)
    private int sortOrder;

    @CreatedDate
    private LocalDateTime createdAt;

    private PlaceImage(Place place, String storageKey, String thumbnailStorageKey, int sortOrder) {
        if (place == null || storageKey == null || storageKey.isBlank()
                || thumbnailStorageKey == null || thumbnailStorageKey.isBlank()
                || sortOrder < 0 || sortOrder > 4) {
            throw new IllegalArgumentException("Invalid place image");
        }
        this.place = place;
        this.storageKey = storageKey;
        this.thumbnailStorageKey = thumbnailStorageKey;
        this.sortOrder = sortOrder;
    }

    public static PlaceImage of(Place place, String storageKey, int sortOrder) {
        return of(place, storageKey, storageKey, sortOrder);
    }

    public static PlaceImage of(Place place, String storageKey, String thumbnailStorageKey, int sortOrder) {
        return new PlaceImage(place, storageKey, thumbnailStorageKey, sortOrder);
    }
}
