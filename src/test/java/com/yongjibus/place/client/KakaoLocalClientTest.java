package com.yongjibus.place.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoLocalClientTest {
    @Test
    void viewportSearchRequestsFoodAndCafeInsideKakaoRectangle() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://dapi.kakao.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoLocalClient client = new KakaoLocalClient(
                builder.defaultHeader("Authorization", "KakaoAK test-key").build(),
                BigDecimal.valueOf(37.2242), BigDecimal.valueOf(127.18766), 5000);
        String baseUrl = "https://dapi.kakao.com/v2/local/search/category.json?";
        String result = "{\"documents\":[]}";
        String rect = "127.186,37.223,127.189,37.225";

        server.expect(requestTo(baseUrl + "category_group_code=FD6&rect=127.186,37.223,127.189,37.225&size=15"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "KakaoAK test-key"))
                .andRespond(withSuccess(result, MediaType.APPLICATION_JSON));
        server.expect(requestTo(baseUrl + "category_group_code=CE7&rect=127.186,37.223,127.189,37.225&size=15"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(result, MediaType.APPLICATION_JSON));

        assertThat(client.searchViewport(37.223, 127.186, 37.225, 127.189)).isEmpty();
        server.verify();
    }

    @Test
    void campusAreaUsesConfiguredCenterAndRadius() {
        KakaoLocalClient client = new KakaoLocalClient(RestClient.builder(), "test-key",
                BigDecimal.valueOf(37.2242), BigDecimal.valueOf(127.18766), 5000);

        assertThat(client.isWithinCampusArea(37.2242, 127.18766)).isTrue();
        assertThat(client.isWithinCampusArea(37.2742, 127.18766)).isFalse();
        assertThat(client.isWithinCampusArea(Double.NaN, 127.18766)).isFalse();
    }

    @Test
    void viewportMustBeOrderedAndFitInsideTheConfiguredCampusArea() {
        KakaoLocalClient client = new KakaoLocalClient(RestClient.builder(), "test-key",
                BigDecimal.valueOf(37.2242), BigDecimal.valueOf(127.18766), 5000);

        assertThat(client.isViewportWithinSearchArea(37.223, 127.186, 37.225, 127.189)).isTrue();
        assertThat(client.isViewportWithinSearchArea(37.225, 127.186, 37.223, 127.189)).isFalse();
        assertThat(client.isViewportWithinSearchArea(37.27, 127.186, 37.28, 127.189)).isFalse();
        assertThat(client.isViewportWithinSearchArea(37.2, 127.1, 37.25, 127.25)).isFalse();
    }
}
