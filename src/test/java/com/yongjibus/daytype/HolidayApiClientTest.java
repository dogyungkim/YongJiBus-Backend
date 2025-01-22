package com.yongjibus.daytype;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.yongjibus.daytype.client.HolidayApiClient;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;

@RestClientTest(HolidayApiClient.class)
class HolidayApiClientTest {

    @Autowired
    private HolidayApiClient holidayApiClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    @DisplayName("휴일 정보를 성공적으로 가져오는 경우")
    void fetchHolidayInfo_Success() throws Exception {
        // given
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

        server.expect(requestTo("http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo?solYear=2024&solMonth=01&serviceKey=G3nugGM7tSyjGHLR7YqbaxEW6C%2Bdl55OKHSzZT8jyiCN6A1IQtR6SjXrA9m5BQyrutIEsDMNb3kt57vagxNczg%3D%3D"))
            .andRespond(withSuccess(sampleXmlResponse, MediaType.APPLICATION_XML));

        // when
        String result = holidayApiClient.fetchHolidayInfo(LocalDate.of(2024, 1, 1));

        // then
        assertThat(result).isEqualTo(sampleXmlResponse);
    }
} 