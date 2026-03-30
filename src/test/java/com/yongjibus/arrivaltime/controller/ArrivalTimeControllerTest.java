package com.yongjibus.arrivaltime.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import com.yongjibus.arrivaltime.controller.dto.GetArrivalTimeRequestDTO;
import com.yongjibus.arrivaltime.domain.ArrivalTime;
import com.yongjibus.arrivaltime.service.ArrivalTimeService;
import com.yongjibus.support.ControllerTestSupport;

@ExtendWith(MockitoExtension.class)
class ArrivalTimeControllerTest extends ControllerTestSupport {

    @Mock
    private ArrivalTimeService arrivalTimeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = createMockMvc(new ArrivalTimeController(arrivalTimeService));
    }

    @Test
    @DisplayName("도착 시간 저장은 요청 DTO를 서비스에 전달한다")
    void saveArrivalTime_ShouldDelegateToService() throws Exception {
        mockMvc.perform(post("/arrivaltime/save")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "busId":1,
                                  "date":"2026-03-30",
                                  "time":"08:30:00",
                                  "isHoliday":false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Success"));
    }

    @Test
    @DisplayName("날짜별 전체 도착 시간 조회는 버스별 목록으로 응답한다")
    void getAllArrivalTime_ShouldReturnGroupedResponse() throws Exception {
        when(arrivalTimeService.getArrivalTimesGroupedByBusId(LocalDate.of(2026, 3, 30)))
                .thenReturn(Map.of(
                        1, List.of(ArrivalTime.builder()
                                .timeId(1)
                                .date(LocalDate.of(2026, 3, 30))
                                .dayOfWeek("MONDAY")
                                .time(LocalTime.of(8, 30))
                                .isHoliday(false)
                                .build())
                ));

        mockMvc.perform(get("/arrivaltime/2026-03-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].busId").value(1))
                .andExpect(jsonPath("$.data[0].arrivalTimes[0]").value("08:30"));
    }

    @Test
    @DisplayName("특정 버스 도착 시간 조회는 시간 목록을 응답한다")
    void getArrivalTime_ShouldReturnTopFiveResponse() throws Exception {
        when(arrivalTimeService.getFiveArrivalTimeByBusIdAndDate(new GetArrivalTimeRequestDTO(1, LocalDate.of(2026, 3, 30))))
                .thenReturn(List.of(ArrivalTime.builder()
                        .timeId(1)
                        .date(LocalDate.of(2026, 3, 30))
                        .dayOfWeek("MONDAY")
                        .time(LocalTime.of(8, 30))
                        .isHoliday(false)
                        .build()));

        mockMvc.perform(get("/arrivaltime/2026-03-30/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].busId").value(1))
                .andExpect(jsonPath("$.data[0].time").value("08:30"));
    }
}
