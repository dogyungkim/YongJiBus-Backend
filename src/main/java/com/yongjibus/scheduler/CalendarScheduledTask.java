package com.yongjibus.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.yongjibus.daytype.service.DayTypeService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CalendarScheduledTask {

    private final DayTypeService dayTypeService;

    // 매달 1일 자정에 실행
    @Scheduled(cron = "0 0 0 1 * ?")
    public void executeMonthlyTask() {
        dayTypeService.refreshCurrentMonthDayInfo();
    }
}
