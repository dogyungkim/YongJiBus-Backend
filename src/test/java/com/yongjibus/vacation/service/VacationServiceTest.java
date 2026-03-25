package com.yongjibus.vacation.service;

import com.yongjibus.vacation.domain.VacationPeriod;
import com.yongjibus.vacation.repository.VacationPeriodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VacationServiceTest {

    @Mock
    private VacationPeriodRepository vacationPeriodRepository;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private VacationService vacationService;

    private VacationPeriod sampleVacationPeriod;

    @BeforeEach
    void setUp() {
        sampleVacationPeriod = VacationPeriod.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 2, 29))
                .build();
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
    @DisplayName("방학 시작일도 방학으로 계산한다")
    void isVacation_OnStartDate() {
        // given
        LocalDate testDate = sampleVacationPeriod.getStartDate();
        when(vacationPeriodRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(testDate, testDate))
                .thenReturn(sampleVacationPeriod);

        // when
        boolean result = vacationService.isVacation(testDate);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("방학 종료일도 방학으로 계산한다")
    void isVacation_OnEndDate() {
        // given
        LocalDate testDate = sampleVacationPeriod.getEndDate();
        when(vacationPeriodRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(testDate, testDate))
                .thenReturn(sampleVacationPeriod);

        // when
        boolean result = vacationService.isVacation(testDate);

        // then
        assertThat(result).isTrue();
    }
} 
