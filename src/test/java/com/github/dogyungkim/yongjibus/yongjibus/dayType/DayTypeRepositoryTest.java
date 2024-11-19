package com.github.dogyungkim.yongjibus.yongjibus.dayType;

import com.github.dogyungkim.yongjibus.yongjibus.dayType.repository.DayTypeRepository;
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
        LocalDate date = LocalDate.now();
        //when
        Boolean dayType = repository.findByDate(date);
        //Then
        assertThat(dayType).isEqualTo(false);
    }
}
