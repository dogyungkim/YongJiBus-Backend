package com.yongjibus.global.infra.gmail;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.google.api.services.gmail.model.ListHistoryResponse;
import com.google.api.services.gmail.model.Message;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class GmailApiServiceTest {

    @Autowired
    private GmailApiService gmailApiService;

    @Test
    @DisplayName("스프링 컨텍스트와 GmailApiService 빈이 생성된다")
    void contextLoads() {
        assertThat(gmailApiService).isNotNull();
    }

    @Test
    @DisplayName("[IT] History ID를 사용하여 메시지 변경 내역을 가져온다")
    void getHistory_integration() throws Exception {
        BigInteger startHistoryId = BigInteger.valueOf(597888);
        ListHistoryResponse history = gmailApiService.getHistory(startHistoryId);
        assertThat(history).isNotNull();
    }

    @Test
    @DisplayName("[IT] 메시지 ID를 사용하여 메시지를 가져온다")
    void getMessage_integration() throws Exception {
        String messageId = "1995c00adf8db665";
        Message message = gmailApiService.getMessage(messageId);
        assertThat(message).isNotNull();
    }
}
