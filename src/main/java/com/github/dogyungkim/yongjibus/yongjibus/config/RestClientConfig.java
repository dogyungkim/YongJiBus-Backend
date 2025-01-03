package com.github.dogyungkim.yongjibus.yongjibus.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient(@Value("${secrets.open_api_key}") String openApiKey) {
        return RestClient.builder()
                .baseUrl("http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService")
                .defaultUriVariables(Map.of("ServiceKey", openApiKey))
                .build();
    }
} 