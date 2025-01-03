package com.github.dogyungkim.yongjibus.yongjibus.daytype;

import com.github.dogyungkim.yongjibus.yongjibus.daytype.model.DateInfo;
import com.github.dogyungkim.yongjibus.yongjibus.daytype.service.DayTypeService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class DayTypeServiceTest {

    @Autowired
    DayTypeService service;


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
