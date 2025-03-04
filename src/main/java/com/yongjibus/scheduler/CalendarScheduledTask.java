package com.yongjibus.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.yongjibus.daytype.repository.InMemoryDayTypeRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CalendarScheduledTask {

    private final InMemoryDayTypeRepository inMemoryDayTypeRepository;

    // 매달 1일 자정에 실행
    @Scheduled(cron = "0 0 0 1 * ?")
    public void executeMonthlyTask() {
        inMemoryDayTypeRepository.setDateData();
    }
}
