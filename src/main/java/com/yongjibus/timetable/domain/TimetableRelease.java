package com.yongjibus.timetable.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "timetable_release")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimetableRelease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long version;

    @Column(nullable = false, updatable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Column(name = "published_at", nullable = false, updatable = false)
    private LocalDateTime publishedAt;
}
