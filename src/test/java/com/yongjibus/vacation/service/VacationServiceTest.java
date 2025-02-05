package com.yongjibus.vacation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yongjibus.vacation.domain.VacationPeriod;
import com.yongjibus.vacation.repository.VacationPeriodRepository;

@ExtendWith(MockitoExtension.class)
class VacationServiceTest {

    @InjectMocks
    private VacationService vacationService;

    @Mock
    private VacationPeriodRepository vacationPeriodRepository;

    @Test
    @DisplayName("방학 기간 저장 테스트")
    void saveVacationPeriodTest() {
        // given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 2, 29);
        VacationPeriod vacationPeriod = VacationPeriod.builder()
            .startDate(startDate)
            .endDate(endDate)
            .vacationDescription("겨울방학")
            .build();

        when(vacationPeriodRepository.save(any(VacationPeriod.class))).thenReturn(vacationPeriod);

        // when
        vacationService.saveVacationPeriod(vacationPeriod);

        // then
        // void 메서드이므로 예외가 발생하지 않으면 성공
    }

    @Test
    @DisplayName("방학 기간 체크 - 방학 기간인 경우")
    void isVacationTest_DuringVacation() {
        // given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 2, 29);
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        VacationPeriod vacationPeriod = VacationPeriod.builder()
            .startDate(startDate)
            .endDate(endDate)
            .vacationDescription("겨울방학")
            .build();

        when(vacationPeriodRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(targetDate,targetDate))
            .thenReturn(vacationPeriod);

        // when
        boolean result = vacationService.isVacation(targetDate);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("방학 기간 체크 - 방학 기간이 아닌 경우")
    void isVacationTest_NotDuringVacation() {
        // given
        LocalDate targetDate = LocalDate.of(2024, 3, 15);

        when(vacationPeriodRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(targetDate,targetDate))
            .thenReturn(null);

        // when
        boolean result = vacationService.isVacation(targetDate);

        // then
        assertThat(result).isFalse();
    }
} 