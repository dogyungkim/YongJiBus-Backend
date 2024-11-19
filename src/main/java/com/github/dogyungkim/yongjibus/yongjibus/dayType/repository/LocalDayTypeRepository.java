package com.github.dogyungkim.yongjibus.yongjibus.dayType.repository;

import org.springframework.stereotype.Repository;


import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class LocalDayTypeRepository implements DayTypeRepository {
    private static final Map<LocalDate,Boolean> store = new ConcurrentHashMap<LocalDate, Boolean>();

    public LocalDayTypeRepository(){
        LocalDate date = LocalDate.now();
        int toZero = date.getDayOfMonth();
        date = date.minusDays(toZero - 1);
        for (int i = 1; i<30; i ++){
            store.put(date,true);
            date = date.plusDays(1);
        }
    }

    public Boolean findByDate(LocalDate date){
        return store.get(date);
    }

    public void setHoliday(){

    }
}
