package com.github.dogyungkim.yongjibus.yongjibus.daytype;

import com.github.dogyungkim.yongjibus.yongjibus.daytype.model.DateInfo;
import com.github.dogyungkim.yongjibus.yongjibus.daytype.repository.DayTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class DayTypeRepositoryTest {

    @Autowired
    DayTypeRepository repository;

    @Test
    void findByDate(){
        //Given
        repository.setDateData();
        LocalDate date = LocalDate.now();
        //when
        DateInfo dayType = repository.findByDate(date);
        //Then
        assertThat(dayType).isEqualTo(false);
    }

    @Test
    void setDate(){
        //Given
        repository.setDateData();
        //when
        DateInfo dayType = repository.findByDate(LocalDate.now());
        //Then
        assertThat(dayType).isNotNull();
    }
}
