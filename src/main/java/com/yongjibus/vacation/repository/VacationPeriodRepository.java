package com.yongjibus.vacation.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yongjibus.vacation.domain.VacationPeriod;

public interface VacationPeriodRepository extends JpaRepository<VacationPeriod, Long> {
    VacationPeriod findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate localDate, LocalDate localDate2);
    VacationPeriod findFirstByOrderByIdDesc();
} 