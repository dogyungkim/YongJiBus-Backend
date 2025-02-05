package com.yongjibus.vacation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.yongjibus.vacation.domain.VacationPeriod;

@DataJpaTest
class VacationPeriodRepositoryTest {

    @Autowired
    private VacationPeriodRepository vacationPeriodRepository;

    @BeforeEach
    void setUp() {
        vacationPeriodRepository.deleteAll();
    }

    @Test
    @DisplayName("방학 기간 저장 및 조회 테스트")
    void saveAndFindVacationPeriodTest() {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 2, 28);
        
        VacationPeriod vacationPeriod = VacationPeriod.builder()
            .startDate(startDate)
            .endDate(endDate)
            .vacationDescription("겨울방학")
            .build();

        // when
        VacationPeriod savedVacationPeriod = vacationPeriodRepository.save(vacationPeriod);

        // then
        assertThat(savedVacationPeriod).isNotNull();
        assertThat(savedVacationPeriod.getStartDate()).isEqualTo(startDate);
        assertThat(savedVacationPeriod.getEndDate()).isEqualTo(endDate);
        assertThat(savedVacationPeriod.getVacationDescription()).isEqualTo("겨울방학");
    }

    @Test
    @DisplayName("특정 날짜의 방학 기간 조회 테스트")
    void findByDateRangeTest() {
        // given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 2, 29);
        LocalDate targetDate = LocalDate.of(2024, 1, 15);
        
        VacationPeriod vacationPeriod = VacationPeriod.builder()
            .startDate(startDate)
            .endDate(endDate)
            .vacationDescription("겨울방학")
            .build();
        
        vacationPeriodRepository.save(vacationPeriod);

        // when
        VacationPeriod foundVacationPeriod = vacationPeriodRepository
            .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(targetDate,targetDate);

        // then
        assertThat(foundVacationPeriod).isNotNull();
        assertThat(foundVacationPeriod.getStartDate()).isEqualTo(startDate);
        assertThat(foundVacationPeriod.getEndDate()).isEqualTo(endDate);
    }

    @Test
    @DisplayName("방학 기간이 아닌 날짜 조회 테스트")
    void findByDateRange_NotInVacationPeriodTest() {
        // given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 2, 29);
        LocalDate targetDate = LocalDate.of(2024, 3, 15); // 방학 기간 이후
        
        VacationPeriod vacationPeriod = VacationPeriod.builder()
            .startDate(startDate)
            .endDate(endDate)
            .vacationDescription("겨울방학")
            .build();
        
        vacationPeriodRepository.save(vacationPeriod);

        // when
        VacationPeriod foundVacationPeriod = vacationPeriodRepository
            .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(targetDate,targetDate);

        // then
        assertThat(foundVacationPeriod).isNull();
    }
} 