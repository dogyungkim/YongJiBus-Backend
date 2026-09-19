package com.yongjibus.timetable.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yongjibus.timetable.domain.TimetableRelease;

public interface TimetableReleaseRepository extends JpaRepository<TimetableRelease, Long> {
    Optional<TimetableRelease> findTopByOrderByVersionDesc();
}
