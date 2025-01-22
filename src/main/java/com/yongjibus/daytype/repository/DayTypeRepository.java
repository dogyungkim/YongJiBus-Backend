package com.yongjibus.daytype.repository;

import java.time.LocalDate;
import java.util.List;

import com.yongjibus.daytype.model.DateInfo;

public interface DayTypeRepository {
    DateInfo findByDate(LocalDate date);
    void setDateData();
    void setHolidayData(List<DateInfo> dateInfoInfoList);
}