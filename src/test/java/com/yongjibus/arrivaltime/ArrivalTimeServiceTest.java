package com.yongjibus.arrivaltime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;

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
import com.yongjibus.exception.NotFoundException;

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
        List<ArrivalTime> result = arrivalTimeService.getArrivalTime(request);

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
        NotFoundException exception = assertThrows(NotFoundException.class, 
            () -> arrivalTimeService.getArrivalTime(request));
        
        assertEquals(
            String.format("버스 ID %d의 %s 도착 시간 정보를 찾을 수 없습니다.", busId, date), 
            exception.getMessage()
        );
    }
}
