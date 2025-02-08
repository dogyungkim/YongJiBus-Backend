package com.yongjibus.daytype.repository;

import java.time.LocalDate;
import java.util.List;

import com.yongjibus.daytype.domain.DateInfo;

public interface DayTypeRepository {
    DateInfo findByDate(LocalDate date);
    void setDateData();
    void setHolidayData(List<DateInfo> dateInfoInfoList);
}