package com.yongjibus.arrivaltime;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.yongjibus.arrivaltime.domain.ArrivalTime;
import com.yongjibus.arrivaltime.repository.ArrivalTimeRepository;
import com.yongjibus.exception.NotFoundException;

@DataJpaTest
public class ArrivalTimeRepositoryTest {
    
    @Autowired
    private ArrivalTimeRepository arrivalTimeRepository;

    @BeforeEach
    void setUp() {
        arrivalTimeRepository.deleteAll();
    }

    @Test
    public void testSaveArrivalTime() {
        // given
        LocalDate date = LocalDate.now();
        ArrivalTime arrivalTime = ArrivalTime.builder()
            .timeId(1)
            .date(date)
            .dayOfWeek(date.getDayOfWeek().toString())
            .time(LocalTime.of(10, 0))
            .isHoliday(false)
            .build();

        // when
        ArrivalTime savedArrivalTime = arrivalTimeRepository.save(arrivalTime);

        // then
        assertNotNull(savedArrivalTime.getId());
        assertEquals(1, savedArrivalTime.getTimeId());
        assertEquals(date, savedArrivalTime.getDate());
    }

    @Test
    public void testFindByTimeIdAndDate() {
        // given
        LocalDate date = LocalDate.of(2025, 1, 28);
        int timeId = 3;
        
        ArrivalTime arrivalTime = ArrivalTime.builder()
            .timeId(timeId)
            .date(date)
            .dayOfWeek(date.getDayOfWeek().toString())
            .time(LocalTime.of(10, 0))
            .isHoliday(false)
            .build();
        
        arrivalTimeRepository.save(arrivalTime);

        // when
        List<ArrivalTime> found = arrivalTimeRepository.findByTimeIdAndDate(timeId, date).orElse(null);

        // then
        assertFalse(found.isEmpty());
        assertEquals(1, found.size());
        assertEquals(timeId, found.get(0).getTimeId());
        assertEquals(date, found.get(0).getDate());
    }
}
