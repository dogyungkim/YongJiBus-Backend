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
class HolidayScheduledTaskTest {

    @Mock
    private DayTypeService dayTypeService;

    @InjectMocks
    private HolidayScheduledTask holidayScheduledTask;

    @Test
    @DisplayName("공휴일 스케줄은 공휴일 정보를 적재한다")
    void fetchHolidayOnSchedule_ShouldLoadHolidayInfo() {
        // when
        holidayScheduledTask.fetchHolidayOnSchedule();

        // then
        verify(dayTypeService).loadHolidayInfo();
    }
}
