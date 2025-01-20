package com.yongjibus.daytype.service;

import java.time.LocalDate;

import com.yongjibus.daytype.model.DateInfo;

public interface DayTypeService {
    DateInfo getDayType(LocalDate date);

    void setHolidayInfo();
}
