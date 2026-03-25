package com.yongjibus.vacation.service;

import com.yongjibus.vacation.domain.VacationPeriod;
import com.yongjibus.vacation.repository.VacationPeriodRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@RequiredArgsConstructor
@Service
@Slf4j
public class VacationService {
    
    private final VacationPeriodRepository vacationPeriodRepository;
    private final CacheManager cacheManager;

    /**
     * 방학 기간을 저장하는 메서드
     * @param vacationPeriod 저장할 방학 기간
     */
    public void saveVacationPeriod(VacationPeriod vacationPeriod) {
        vacationPeriodRepository.save(vacationPeriod);
        evictDayInfoCache();
    }

    /**
     * 현재 설정된 가장 최근의 방학 기간을 조회하는 메서드
     * @return VacationPeriod 가장 최근에 설정된 방학 기간 정보, 없을 경우 null 반환
     */
    public VacationPeriod getCurrentVacation() {
        return vacationPeriodRepository.findFirstByOrderByIdDesc();
    }
    
    /**
     * 특정 날짜가 방학 기간인지 확인하는 메서드
     * @param date 확인할 날짜
     * @return boolean 방학 기간 여부
     */
    public boolean isVacation(LocalDate date) {
        VacationPeriod vacationPeriod = vacationPeriodRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(date,date);
        return vacationPeriod != null
            && !date.isBefore(vacationPeriod.getStartDate())
            && !date.isAfter(vacationPeriod.getEndDate());
    }

    private void evictDayInfoCache() {
        Cache dayInfoCache = cacheManager.getCache("dayInfo");
        if (dayInfoCache != null) {
            dayInfoCache.clear();
        }
    }
} 
