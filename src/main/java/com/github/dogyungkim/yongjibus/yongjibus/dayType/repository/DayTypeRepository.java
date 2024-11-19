package com.github.dogyungkim.yongjibus.yongjibus.dayType.repository;

import java.time.LocalDate;

public interface DayTypeRepository {
    Boolean findByDate(LocalDate date);
}
