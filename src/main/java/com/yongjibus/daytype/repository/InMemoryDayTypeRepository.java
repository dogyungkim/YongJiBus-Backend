package com.yongjibus.daytype.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.yongjibus.daytype.domain.DateInfo;
import com.yongjibus.exception.DateInfoNotFoundException;

import jakarta.annotation.PostConstruct;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

@Repository
@RequiredArgsConstructor
public class InMemoryDayTypeRepository implements DayTypeRepository {

    // List로 HolidayInfo를 저장
    private static final List<DateInfo> store = new CopyOnWriteArrayList<>();

    @PostConstruct
    private void init(){
        setDateData();
    }

    /**
     * 특정 날짜를 기준으로 HolidayInfo를 검색
     * @param date 검색할 날짜
     * @return Boolean 해당 날짜가 공휴일인지 여부
     */
    @Override
    public DateInfo findByDate(LocalDate date) {
        for (DateInfo dateInfo : store) {
            if (dateInfo.getDate().equals(date)) {
                return dateInfo;
            }
        }
        throw new DateInfoNotFoundException(date.toString());
    }

    /**
     * 현재 월의 날짜 데이터를 메모리에 저장
     */
    @Override
    public void setDateData() {
        List<DateInfo> currentMonthDates = IntStream.rangeClosed(1, getLastDayOfMonth())
                .mapToObj(day -> {
                    LocalDate date = LocalDate.now().withDayOfMonth(day);
                    boolean isWeekend = isWeekend(date);
                    return new DateInfo(date, isWeekend, isWeekend ? "주말" : "평일" );
                })
                .toList();

        store.clear();
        store.addAll(currentMonthDates);
    }

    /**
     * 공휴일 데이터를 List에 저장
     * @param dateInfoList 저장할 HolidayInfo 리스트
     */
    @Override
    public void setHolidayData(List<DateInfo> dateInfoList) {
        int storeSize = store.size();

        for (DateInfo dateInfo : dateInfoList) {
            for (int i = 0; i < storeSize; i++) {
                DateInfo storedInfo = store.get(i);
                if (storedInfo.getDate().equals(dateInfo.getDate())) {
                    store.set(i, dateInfo);
                    break;
                }
            }
        }
    }

    /**
     * 현재 월의 마지막 일을 반환
     * @return int 현재 월의 마지막 일
     */
    private int getLastDayOfMonth() {
        return LocalDate.now().lengthOfMonth();
    }

    /**
     * 특정 날짜가 주말인지 확인
     * @param date 확인할 날짜
     * @return boolean 주말 여부
     */
    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}