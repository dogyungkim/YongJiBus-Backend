package com.yongjibus.daytype;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.yongjibus.daytype.model.DateInfo;
import com.yongjibus.daytype.repository.DayTypeRepository;
import com.yongjibus.exception.DateInfoNotFoundException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class InMemoryDayTypeRepositoryTest {

    @Autowired
    DayTypeRepository repository;

    @Test
    void findByDate_WhenDateExists_ShouldReturnDateInfo() {
        //Given
        repository.setDateData();
        LocalDate date = LocalDate.now();
        //when
        DateInfo dayType = repository.findByDate(date);
        //Then
        assertThat(dayType).isNotNull();
    }

    @Test
    void setDateData_WhenCalled_ShouldInitializeDateInfo() {
        //Given
        repository.setDateData();
        //when
        DateInfo dayType = repository.findByDate(LocalDate.now());
        //Then
        assertThat(dayType).isNotNull();
        assertThat(dayType).isInstanceOf(DateInfo.class);
    }

    @Test
    void findByDate_WhenRepositoryIsEmpty_ShouldThrowException() {
        // Given
        LocalDate today = LocalDate.now().plusYears(1);

        // When & Then
        assertThatThrownBy(() -> repository.findByDate(today))
                .isInstanceOf(DateInfoNotFoundException.class)
                .hasMessageContaining(today.toString());
    }
}