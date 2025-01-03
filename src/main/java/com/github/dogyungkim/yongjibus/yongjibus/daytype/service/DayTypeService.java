package com.github.dogyungkim.yongjibus.yongjibus.daytype.service;

import com.github.dogyungkim.yongjibus.yongjibus.daytype.model.DateInfo;

import java.time.LocalDate;

public interface DayTypeService {
    DateInfo getDayType(LocalDate date);

    void setHolidayInfo();
}
