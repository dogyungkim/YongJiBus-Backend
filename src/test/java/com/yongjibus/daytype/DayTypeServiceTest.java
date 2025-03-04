package com.yongjibus.daytype;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yongjibus.daytype.client.HolidayApiClient;
import com.yongjibus.daytype.domain.DateInfo;
import com.yongjibus.daytype.repository.DayTypeRepository;
import com.yongjibus.daytype.service.DayTypeService;
import com.yongjibus.vacation.service.VacationService;

@ExtendWith(MockitoExtension.class)
class DayTypeServiceTest {

    @Mock
    private DayTypeRepository dayTypeRepository;

    @Mock
    private HolidayApiClient holidayApiClient;
    
    @Mock
    private VacationService vacationService;

    @InjectMocks
    private DayTypeService dayTypeService;

    private final LocalDate testDate = LocalDate.of(2024, 1, 1);
    String sampleXmlResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <response>
                <header>
                    <resultCode>00</resultCode>
                    <resultMsg>NORMAL SERVICE.</resultMsg>
                </header>
                <body>
                    <items>
                        <item>
                            <dateName>신정</dateName>
                            <locdate>20240101</locdate>
                            <isHoliday>Y</isHoliday>
                        </item>
                    </items>
                </body>
            </response>
            """;

    @Test
    @DisplayName("특정 날짜의 DateInfo 조회 테스트")
    void findDayInfo_ShouldReturnDateInfo() {
        // given
        DateInfo expectedDateInfo = new DateInfo(testDate, true, "신정");
        when(dayTypeRepository.findByDate(testDate)).thenReturn(expectedDateInfo);
        when(vacationService.isVacation(testDate)).thenReturn(false);

        // when
        DateInfo result = dayTypeService.findDayInfo(testDate);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDate()).isEqualTo(testDate);
        assertThat(result.isHoliday()).isTrue();
        assertThat(result.getDateKind()).isEqualTo("신정");
    }

    @Test
    @DisplayName("공휴일 정보 설정 테스트")
    void setHolidayInfo_ShouldSetHolidayData() throws Exception {
        // given
        when(holidayApiClient.fetchHolidayInfo(any(LocalDate.class))).thenReturn(sampleXmlResponse);

        // when
        dayTypeService.loadHolidayInfo();

        // then
        verify(dayTypeRepository).setHolidayData(any());
    }

    @Test
    @DisplayName("API 호출 실패시 예외 발생 테스트")
    void loadHolidayInfo_ShouldThrowException_WhenApiFails() {
        assertThatThrownBy(() -> dayTypeService.loadHolidayInfo())
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to get holiday data");
    }
}
