package com.yongjibus.daytype;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

import com.yongjibus.daytype.client.HolidayApiClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;

@RestClientTest(HolidayApiClient.class)
class HolidayApiClientTest {

    @Autowired
    private HolidayApiClient holidayApiClient;

    @Autowired
    private MockRestServiceServer server;

    @Value("${secrets.open_api_key}")
    private String openApiKey;

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

        String encodedOpenApiKey = URLEncoder.encode(openApiKey, StandardCharsets.UTF_8);
        String expectedUrl = UriComponentsBuilder
            .fromHttpUrl("http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo")
            .queryParam("solYear", "2024")
            .queryParam("solMonth", "01")
            .queryParam("serviceKey", encodedOpenApiKey)
            .build()
            .toUriString();
        server.expect(requestTo(expectedUrl))
            .andRespond(withSuccess(sampleXmlResponse, MediaType.APPLICATION_XML));

        // when
        String result = holidayApiClient.fetchHolidayInfo(LocalDate.of(2024, 1, 1));

        // then
        assertThat(result).isEqualTo(sampleXmlResponse);
    }

    @Test
    @DisplayName("휴일 정보 조회가 실패하면 런타임 예외로 감싼다")
    void fetchHolidayInfo_WhenApiFails_ShouldWrapException() {
        // given
        String encodedOpenApiKey = URLEncoder.encode(openApiKey, StandardCharsets.UTF_8);
        String expectedUrl = UriComponentsBuilder
            .fromHttpUrl("http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo")
            .queryParam("solYear", "2024")
            .queryParam("solMonth", "01")
            .queryParam("serviceKey", encodedOpenApiKey)
            .build()
            .toUriString();
        server.expect(requestTo(expectedUrl))
            .andRespond(withServerError());

        // when & then
        assertThatThrownBy(() -> holidayApiClient.fetchHolidayInfo(LocalDate.of(2024, 1, 1)))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to fetch holiday data");
    }
} 
