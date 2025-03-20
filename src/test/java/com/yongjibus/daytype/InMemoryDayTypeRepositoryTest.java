package com.yongjibus.daytype;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.yongjibus.daytype.domain.DateInfo;
import com.yongjibus.daytype.repository.DayTypeRepository;
import com.yongjibus.global.error.exception.DateInfoNotFoundException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class InMemoryDayTypeRepositoryTest {

    @Autowired
    DayTypeRepository repository;

    @Test
    @DisplayName("날짜가 존재할 때 DateInfo를 반환해야 한다")
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
    @DisplayName("레포지토리가 비어 있을 때 예외를 던져야 한다")
    void findByDate_WhenRepositoryIsEmpty_ShouldThrowException() {
        // Given
        // 1년 뒤의 날짜
        LocalDate today = LocalDate.now().plusYears(1);

        // When & Then
        assertThatThrownBy(() -> repository.findByDate(today))
                .isInstanceOf(DateInfoNotFoundException.class)
                .hasMessageContaining(today.toString());
    }
}