package com.github.dogyungkim.yongjibus.yongjibus.daytype.repository;

import com.github.dogyungkim.yongjibus.yongjibus.daytype.model.DateInfo;

import java.time.LocalDate;
import java.util.List;

public interface DayTypeRepository {
    DateInfo findByDate(LocalDate date);
    void setDateData();
    void setHolidayData(List<DateInfo> dateInfoInfoList);
}