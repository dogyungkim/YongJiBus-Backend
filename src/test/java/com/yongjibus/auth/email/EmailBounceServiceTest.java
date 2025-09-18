package com.yongjibus.auth.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SpringBootTest
public class EmailBounceServiceTest {

    @Autowired
    private EmailBounceService emailBounceService;

    @Test
    @DisplayName("바운스 메일 처리 테스트")
    void processBounceNotificationTest() {
        emailBounceService.processBounceNotification("597888");
    }
    
}
