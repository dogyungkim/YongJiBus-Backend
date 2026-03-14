package com.yongjibus.global.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class YongJiResponseTest {

    @Test
    @DisplayName("에러 응답은 전달받은 HTTP 상태 코드를 그대로 사용한다")
    void error_ShouldUseProvidedHttpStatus() {
        // when
        ResponseEntity<YongJiResponse<String>> response = YongJiResponse.error(404, "not found");

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getData()).isEqualTo("not found");
    }
}
