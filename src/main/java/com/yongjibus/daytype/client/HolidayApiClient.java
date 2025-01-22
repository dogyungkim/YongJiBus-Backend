package com.yongjibus.daytype.client;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Component
public class HolidayApiClient {

    private final RestClient restClient;

    public HolidayApiClient(RestClient.Builder builder, @Value("${secrets.open_api_key}") String openApiKey) {
        this.restClient = builder.baseUrl("http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService")
                .defaultUriVariables(Map.of("ServiceKey", openApiKey))
                .build();
    }

    public String fetchHolidayInfo(LocalDate date) {
        try {
            byte[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/getRestDeInfo")
                            .queryParam("solYear", Integer.toString(date.getYear()))
                            .queryParam("solMonth", String.format("%02d", date.getMonthValue()))
                            .queryParam("serviceKey", "{ServiceKey}")
                            .build())
                    .retrieve()
                    .toEntity(byte[].class)
                    .getBody();

            return new String(response, "UTF-8");
        } catch (Exception e) {
            log.error("Failed to fetch holiday data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch holiday data: " + e.getMessage());
        }
    }
} 