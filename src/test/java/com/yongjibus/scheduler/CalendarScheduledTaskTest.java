package com.yongjibus.scheduler;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yongjibus.daytype.service.DayTypeService;

@ExtendWith(MockitoExtension.class)
class CalendarScheduledTaskTest {

    @Mock
    private DayTypeService dayTypeService;

    @InjectMocks
    private CalendarScheduledTask calendarScheduledTask;

    @Test
    @DisplayName("월간 캘린더 스케줄은 현재 달 날짜 정보를 새로고침한다")
    void executeMonthlyTask_ShouldRefreshCurrentMonthDayInfo() {
        // when
        calendarScheduledTask.executeMonthlyTask();

        // then
        verify(dayTypeService).refreshCurrentMonthDayInfo();
    }
}
