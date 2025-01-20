package com.yongjibus.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.yongjibus.daytype.service.DayTypeService;

@Component
@RequiredArgsConstructor
@Slf4j
public class HolidayScheduledTask {

    private final DayTypeService service;
    
    /**
     * 매일 오전 6시에 공휴일 정보를 가져오는 스케줄러
     * 공공데이터 포털 API를 통해 공휴일 정보를 조회하고 메모리에 저장
     * 
     * @Scheduled(cron = "0 0 6 * * *") 
     * - 초(0) 분(0) 시(6) 일(*) 월(*) 요일(*)
     * - 매일 오전 6시에 실행
     */
   @Scheduled(cron = "0 0 6 * * *")
   public void fetchHolidayOnSchedule(){
       log.info("Holiday ScheduledTask started at {}", LocalDateTime.now());
       service.setHolidayInfo();
   }
}
