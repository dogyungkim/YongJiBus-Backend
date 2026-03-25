package com.yongjibus.daytype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.yongjibus.daytype.client.HolidayApiClient;
import com.yongjibus.daytype.domain.DateInfo;
import com.yongjibus.daytype.repository.DayTypeRepository;
import com.yongjibus.daytype.repository.InMemoryDayTypeRepository;
import com.yongjibus.daytype.service.DayTypeService;
import com.yongjibus.global.config.cache.CacheConfig;
import com.yongjibus.vacation.domain.VacationPeriod;
import com.yongjibus.vacation.repository.VacationPeriodRepository;
import com.yongjibus.vacation.service.VacationService;

class DayTypeServiceCachingTest {

    private AnnotationConfigApplicationContext context;
    private HolidayApiClient holidayApiClient;
    private VacationPeriodRepository vacationPeriodRepository;

    private DayTypeService dayTypeService;
    private VacationService vacationService;

    @BeforeEach
    void setUp() {
        holidayApiClient = mock(HolidayApiClient.class);
        vacationPeriodRepository = mock(VacationPeriodRepository.class);

        context = new AnnotationConfigApplicationContext();
        context.register(TestConfig.class);
        context.getBeanFactory().registerSingleton("holidayApiClient", holidayApiClient);
        context.getBeanFactory().registerSingleton("vacationPeriodRepository", vacationPeriodRepository);
        context.refresh();

        dayTypeService = context.getBean(DayTypeService.class);
        vacationService = context.getBean(VacationService.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    @DisplayName("공휴일 데이터를 다시 적재하면 기존 dayInfo 캐시가 비워져야 한다")
    void loadHolidayInfoShouldEvictCachedDayInfo() {
        // given
        LocalDate date = LocalDate.now().withDayOfMonth(1);
        DateInfo beforeRefresh = dayTypeService.findDayInfo(date);
        when(holidayApiClient.fetchHolidayInfo(any(LocalDate.class)))
                .thenReturn(sampleXml(date, "테스트공휴일"));

        // when
        dayTypeService.loadHolidayInfo();
        DateInfo afterRefresh = dayTypeService.findDayInfo(date);

        // then
        assertThat(beforeRefresh.getDateKind()).isNotEqualTo("테스트공휴일");
        assertThat(afterRefresh.isHoliday()).isTrue();
        assertThat(afterRefresh.getDateKind()).isEqualTo("테스트공휴일");
    }

    @Test
    @DisplayName("방학 기간 저장 후 기존 dayInfo 캐시가 비워져야 한다")
    void saveVacationPeriodShouldEvictCachedDayInfo() {
        // given
        LocalDate date = LocalDate.now().withDayOfMonth(Math.min(2, LocalDate.now().lengthOfMonth()));
        VacationPeriod vacationPeriod = VacationPeriod.builder()
                .startDate(date.minusDays(1))
                .endDate(date.plusDays(1))
                .vacationDescription("테스트 방학")
                .build();

        when(vacationPeriodRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date))
                .thenReturn(null);

        DateInfo beforeSave = dayTypeService.findDayInfo(date);

        when(vacationPeriodRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date))
                .thenReturn(vacationPeriod);

        // when
        vacationService.saveVacationPeriod(vacationPeriod);
        DateInfo afterSave = dayTypeService.findDayInfo(date);

        // then
        assertThat(beforeSave.getDateKind()).isNotEqualTo("방학");
        assertThat(afterSave.isHoliday()).isTrue();
        assertThat(afterSave.getDateKind()).isEqualTo("방학");
    }

    private String sampleXml(LocalDate date, String holidayName) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <response>
                    <header>
                        <resultCode>00</resultCode>
                        <resultMsg>NORMAL SERVICE.</resultMsg>
                    </header>
                    <body>
                        <items>
                            <item>
                                <dateName>%s</dateName>
                                <locdate>%s</locdate>
                                <isHoliday>Y</isHoliday>
                            </item>
                        </items>
                    </body>
                </response>
                """.formatted(holidayName, date.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE));
    }

    @Configuration
    @EnableCaching
    @Import(CacheConfig.class)
    static class TestConfig {

        @Bean
        DayTypeRepository dayTypeRepository() {
            return new InMemoryDayTypeRepository();
        }

        @Bean
        VacationService vacationService(
                VacationPeriodRepository vacationPeriodRepository,
                org.springframework.cache.CacheManager cacheManager) {
            return new VacationService(vacationPeriodRepository, cacheManager);
        }

        @Bean
        DayTypeService dayTypeService(
                HolidayApiClient holidayApiClient,
                DayTypeRepository dayTypeRepository,
                VacationService vacationService,
                org.springframework.cache.CacheManager cacheManager) {
            return new DayTypeService(holidayApiClient, dayTypeRepository, vacationService, cacheManager);
        }
    }
}
