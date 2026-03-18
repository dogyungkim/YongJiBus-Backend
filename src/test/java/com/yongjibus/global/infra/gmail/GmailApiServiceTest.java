package com.yongjibus.global.infra.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class GmailApiServiceTest {

    @Autowired
    private GmailApiService gmailApiService;

    @Test
    @DisplayName("스프링 컨텍스트와 GmailApiService 빈이 생성된다")
    void contextLoads() {
        assertThat(gmailApiService).isNotNull();
        assertThat(gmailApiService.isConfigured()).isFalse();
    }

    @Test
    @DisplayName("Gmail 설정이 비활성화되어 있으면 watch 등록을 건너뛴다")
    void watchSkippedWhenDisabled() {
        assertThatNoException().isThrownBy(() -> gmailApiService.watchBounceMailBox());
    }

    // @Test
    // @DisplayName("[IT] History ID를 사용하여 메시지 변경 내역을 가져온다")
    // void getHistory_integration() throws Exception {
    //     BigInteger startHistoryId = BigInteger.valueOf(608458);
    //     ListHistoryResponse history = gmailApiService.getHistory(startHistoryId);
    //     assertThat(history).isNotNull();
    // }

    // @Test
    // @DisplayName("[IT] 메시지 ID를 사용하여 메시지를 가져온다")
    // void getMessage_integration() throws Exception {
    //     String messageId = "1997677b68e38d3d";
    //     Message message = gmailApiService.getMessage(messageId);
    //     assertThat(message).isNotNull();
    // }
}
