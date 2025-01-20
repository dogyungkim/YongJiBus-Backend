package com.yongjibus.daytype;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.yongjibus.daytype.model.DateInfo;
import com.yongjibus.daytype.service.DayTypeService;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class InMemoryTypeServiceTest {

    @Autowired
    private DayTypeService service;

    @Test
    void getHolidayData(){
        //given
        List<DateInfo> dateInfoList;
        //when
        service.setHolidayInfo();
        //then
        DateInfo isHoliday = service.getDayType(LocalDate.now());
        assertThat(isHoliday).isNotNull();
    }
}
