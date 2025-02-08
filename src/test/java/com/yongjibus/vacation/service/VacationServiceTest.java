package com.yongjibus.vacation.service;

import com.yongjibus.vacation.domain.VacationPeriod;
import com.yongjibus.vacation.repository.VacationPeriodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
class VacationServiceTest {

    @MockBean
    private VacationPeriodRepository vacationPeriodRepository;

    @Autowired
    private VacationService vacationService;

    private VacationPeriod sampleVacationPeriod;

    @BeforeEach
    void setUp() {
        sampleVacationPeriod = VacationPeriod.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 2, 29))
                .build();
        vacationService.clearCache();
    }

    @Test
    @DisplayName("방학 기간 저장 테스트")
    void saveVacationPeriod() {
        // given
        when(vacationPeriodRepository.save(any(VacationPeriod.class))).thenReturn(sampleVacationPeriod);

        // when
        vacationService.saveVacationPeriod(sampleVacationPeriod);

        // then
        verify(vacationPeriodRepository, times(1)).save(sampleVacationPeriod);
    }

    @Test
    @DisplayName("현재 방학 기간 조회 테스트")
    void getCurrentVacation() {
        // given
        when(vacationPeriodRepository.findFirstByOrderByIdDesc()).thenReturn(sampleVacationPeriod);

        // when
        VacationPeriod result = vacationService.getCurrentVacation();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStartDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(result.getEndDate()).isEqualTo(LocalDate.of(2024, 2, 29));
    }

    @Test
    @DisplayName("방학 기간 확인 테스트 - 방학 기간인 경우")
    void isVacation_DuringVacation() {
        // given
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        when(vacationPeriodRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(testDate, testDate))
                .thenReturn(sampleVacationPeriod);

        // when
        boolean result = vacationService.isVacation(testDate);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("방학 기간 확인 테스트 - 방학 기간이 아닌 경우")
    void isVacation_NotDuringVacation() {
        // given
        LocalDate testDate = LocalDate.of(2024, 3, 1);
        when(vacationPeriodRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(testDate, testDate))
                .thenReturn(null);

        // when
        boolean result = vacationService.isVacation(testDate);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getCurrentVacation 캐시 동작 테스트")
    void getCurrentVacation_WithCaching() {
        // given
        when(vacationPeriodRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(any(LocalDate.class), any(LocalDate.class))).thenReturn(sampleVacationPeriod);
        LocalDate testDate = LocalDate.of(2024,1,15);

        // when
        boolean result = vacationService.isVacation(testDate);
        boolean result2 = vacationService.isVacation(testDate);

        // then
        verify(vacationPeriodRepository, times(1)).findByStartDateLessThanEqualAndEndDateGreaterThanEqual(any(LocalDate.class), any(LocalDate.class)); // 캐시로 인해 repository는 한 번만 호출됨
        assertThat(result).isEqualTo(result2);
    }
} 