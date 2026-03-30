package com.yongjibus.vacation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.VacationException;
import com.yongjibus.support.ControllerTestSupport;
import com.yongjibus.vacation.domain.VacationPeriod;
import com.yongjibus.vacation.service.VacationService;

@ExtendWith(MockitoExtension.class)
class VacationControllerTest extends ControllerTestSupport {

    @Mock
    private VacationService vacationService;

    private MockMvc mockMvc;
    private VacationPeriod currentVacation;

    @BeforeEach
    void setUp() {
        mockMvc = createMockMvc(new VacationController(vacationService));
        currentVacation = VacationPeriod.builder()
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .vacationDescription("여름방학")
                .build();
    }

    @Test
    @DisplayName("방학 폼 조회는 현재 방학 정보와 빈 폼을 모델에 담는다")
    void showVacationForm_ShouldPopulateModel() throws Exception {
        when(vacationService.getCurrentVacation()).thenReturn(currentVacation);

        mockMvc.perform(get("/vacation"))
                .andExpect(status().isOk())
                .andExpect(view().name("vacation"))
                .andExpect(model().attributeExists("currentVacation"))
                .andExpect(model().attributeExists("vacationForm"));
    }

    @Test
    @DisplayName("방학 저장 성공 시 성공 메시지와 현재 방학 정보를 다시 보여준다")
    void saveVacation_ShouldReturnSuccessMessage() throws Exception {
        when(vacationService.getCurrentVacation()).thenReturn(currentVacation);

        mockMvc.perform(post("/vacation/vacation-period")
                        .param("startDate", "2026-07-01")
                        .param("endDate", "2026-08-31")
                        .param("vacationDescription", "여름방학"))
                .andExpect(status().isOk())
                .andExpect(view().name("vacation"))
                .andExpect(model().attribute("message", "방학 기간이 성공적으로 저장되었습니다."))
                .andExpect(model().attribute("messageType", "success"))
                .andExpect(model().attributeExists("currentVacation"));

        verify(vacationService).saveVacationPeriod(any(VacationPeriod.class));
    }

    @Test
    @DisplayName("폼 검증에 실패하면 저장하지 않고 같은 화면을 반환한다")
    void saveVacation_WhenValidationFails_ShouldReturnFormView() throws Exception {
        mockMvc.perform(post("/vacation/vacation-period")
                        .param("startDate", "")
                        .param("endDate", "2026-08-31")
                        .param("vacationDescription", "여름방학"))
                .andExpect(status().isOk())
                .andExpect(view().name("vacation"));

        verify(vacationService, never()).saveVacationPeriod(any(VacationPeriod.class));
    }

    @Test
    @DisplayName("방학 저장 중 도메인 예외가 나면 에러 메시지를 모델에 담는다")
    void saveVacation_WhenVacationExceptionOccurs_ShouldReturnErrorMessage() throws Exception {
        when(vacationService.getCurrentVacation()).thenReturn(currentVacation);
        doThrow(new VacationException(ErrorCode.OVERLAPPING_VACATION_PERIOD))
                .when(vacationService).saveVacationPeriod(any(VacationPeriod.class));

        mockMvc.perform(post("/vacation/vacation-period")
                        .param("startDate", "2026-07-01")
                        .param("endDate", "2026-08-31")
                        .param("vacationDescription", "여름방학"))
                .andExpect(status().isOk())
                .andExpect(view().name("vacation"))
                .andExpect(model().attribute("message", "기존 방학 기간과 겹칠 수 없습니다."))
                .andExpect(model().attribute("messageType", "error"))
                .andExpect(model().attributeExists("currentVacation"));
    }
}
