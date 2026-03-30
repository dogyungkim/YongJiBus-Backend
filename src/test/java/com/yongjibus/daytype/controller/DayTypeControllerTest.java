package com.yongjibus.daytype.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import com.yongjibus.daytype.domain.DateInfo;
import com.yongjibus.daytype.service.DayTypeService;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.DateInfoNotFoundException;
import com.yongjibus.support.ControllerTestSupport;

@ExtendWith(MockitoExtension.class)
class DayTypeControllerTest extends ControllerTestSupport {

    @Mock
    private DayTypeService dayTypeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = createMockMvc(new DayTypeController(dayTypeService));
    }

    @Test
    @DisplayName("날짜 유형 조회는 서비스 결과를 응답 DTO로 변환한다")
    void getDayType_ShouldReturnDateInfo() throws Exception {
        when(dayTypeService.findDayInfo(LocalDate.of(2026, 3, 30)))
                .thenReturn(DateInfo.builder()
                        .date(LocalDate.of(2026, 3, 30))
                        .isHoliday(false)
                        .dateKind("평일")
                        .build());

        mockMvc.perform(get("/day").param("date", "2026-03-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.date").value("2026-03-30"))
                .andExpect(jsonPath("$.data.isHoliday").value(false))
                .andExpect(jsonPath("$.data.dateKind").value("평일"));
    }

    @Test
    @DisplayName("날짜 정보를 찾지 못하면 공통 에러 응답으로 감싼다")
    void getDayType_WhenDateInfoIsMissing_ShouldReturnErrorResponse() throws Exception {
        when(dayTypeService.findDayInfo(LocalDate.of(2026, 3, 30)))
                .thenThrow(new DateInfoNotFoundException(ErrorCode.DATE_INFO_NOT_FOUND, "2026-03-30"));

        mockMvc.perform(get("/day").param("date", "2026-03-30"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.data").value("2026-03-30의 날짜 정보를 찾을 수 없습니다."));
    }
}
