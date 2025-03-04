package com.yongjibus.arrivaltime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yongjibus.arrivaltime.domain.ArrivalTime;
import com.yongjibus.arrivaltime.domain.SaveArrivalTimeRequestDTO;
import com.yongjibus.arrivaltime.domain.GetArrivalTimeRequestDTO;
import com.yongjibus.arrivaltime.repository.ArrivalTimeRepository;
import com.yongjibus.arrivaltime.service.ArrivalTimeService;
import com.yongjibus.exception.DateInfoNotFoundException;

@ExtendWith(MockitoExtension.class)
public class ArrivalTimeServiceTest {

    @Mock
    private ArrivalTimeRepository arrivalTimeRepository;

    @InjectMocks
    private ArrivalTimeService arrivalTimeService;

    @Test
    public void saveArrivalTime_ShouldSaveTimeInfo() {
        // given
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();
        SaveArrivalTimeRequestDTO request = new SaveArrivalTimeRequestDTO(1, date, time, false);
        
        // when
        arrivalTimeService.saveArrivalTime(request);

        // then
        verify(arrivalTimeRepository).save(any(ArrivalTime.class));
    }

    @Test
    public void getArrivalTime_WhenDataExists_ShouldReturnList() {
        // given
        LocalDate date = LocalDate.of(2025, 1, 28);
        int busId = 3;
        GetArrivalTimeRequestDTO request = new GetArrivalTimeRequestDTO(busId, date);
        
        List<ArrivalTime> expectedList = List.of(
            ArrivalTime.builder()
                .timeId(busId)
                .date(date)
                .dayOfWeek(date.getDayOfWeek().toString())
                .isHoliday(false)
                .build()
        );
        
        when(arrivalTimeRepository.findByTimeIdAndDate(busId, date)).thenReturn(expectedList);

        // when
        List<ArrivalTime> result = arrivalTimeService.getArrivalTimeByBusIdAndDate(request);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(busId, result.get(0).getTimeId());
        assertEquals(date, result.get(0).getDate());
    }

    @Test
    public void getArrivalTime_WhenNoData_ShouldThrowNotFoundException() {
        // given
        LocalDate date = LocalDate.of(2025, 1, 28);
        int busId = 3;
        GetArrivalTimeRequestDTO request = new GetArrivalTimeRequestDTO(busId, date);
        
        when(arrivalTimeRepository.findByTimeIdAndDate(busId, date)).thenReturn(new ArrayList<>());

        // when & then
        DateInfoNotFoundException exception = assertThrows(DateInfoNotFoundException.class,
            () -> arrivalTimeService.getArrivalTimeByBusIdAndDate(request));
        
        assertEquals(
            String.format("실제 버스 도착 시간 정보가 없습니다.", busId, date), 
            exception.getMessage()
        );
    }

    @Test
    public void getArrivalTimesGroupedByBusId_WhenDataExists_ShouldReturnGroupedData() {
        // given
        LocalDate date = LocalDate.of(2025, 1, 28);
        int busId1 = 1;
        int busId2 = 2;
        
        List<ArrivalTime> arrivalTimes = Arrays.asList(
            ArrivalTime.builder()
                .timeId(busId1)
                .date(date)
                .dayOfWeek(date.getDayOfWeek().toString())
                .time(LocalTime.of(10, 0))
                .isHoliday(false)
                .build(),
            ArrivalTime.builder()
                .timeId(busId1)
                .date(date)
                .dayOfWeek(date.getDayOfWeek().toString())
                .time(LocalTime.of(10, 30))
                .isHoliday(false)
                .build(),
            ArrivalTime.builder()
                .timeId(busId2)
                .date(date)
                .dayOfWeek(date.getDayOfWeek().toString())
                .time(LocalTime.of(11, 0))
                .isHoliday(false)
                .build(),
            ArrivalTime.builder()
                .timeId(busId2)
                .date(date)
                .dayOfWeek(date.getDayOfWeek().toString())
                .time(LocalTime.of(11, 30))
                .isHoliday(false)
                .build()
        );
        
        when(arrivalTimeRepository.findByDate(date)).thenReturn(arrivalTimes);

        // when
        Map<Integer, List<ArrivalTime>> result = arrivalTimeService.getArrivalTimesGroupedByBusId(date);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey(busId1));
        assertTrue(result.containsKey(busId2));
        assertEquals(2, result.get(busId1).size());
        assertEquals(2, result.get(busId2).size());
        assertEquals(arrivalTimes.get(0).getTime(), result.get(busId1).get(0).getTime());
        assertEquals(arrivalTimes.get(1).getTime(), result.get(busId1).get(1).getTime());
        assertEquals(arrivalTimes.get(2).getTime(), result.get(busId2).get(0).getTime());
        assertEquals(arrivalTimes.get(3).getTime(), result.get(busId2).get(1).getTime());
    }
}