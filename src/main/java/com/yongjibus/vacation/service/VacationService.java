package com.yongjibus.vacation.service;

import com.yongjibus.vacation.domain.VacationPeriod;
import com.yongjibus.vacation.repository.VacationPeriodRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@RequiredArgsConstructor
@Service
@Slf4j
public class VacationService {
    
    private final VacationPeriodRepository vacationPeriodRepository;

    /**
     * 방학 기간을 저장하는 메서드
     * @param vacationPeriod 저장할 방학 기간
     */
    public void saveVacationPeriod(VacationPeriod vacationPeriod) {
        vacationPeriodRepository.save(vacationPeriod);
    }
    
    /**
     * 특정 날짜가 방학 기간인지 확인하는 메서드
     * @param date 확인할 날짜
     * @return boolean 방학 기간 여부
     */
    @Cacheable(value = "vacationStatus", key = "#date")
    public boolean isVacation(LocalDate date) {
        VacationPeriod vacationPeriod = vacationPeriodRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(date,date);
        return vacationPeriod != null && date.isAfter(vacationPeriod.getStartDate()) && date.isBefore(vacationPeriod.getEndDate());
    }
} 