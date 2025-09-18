package com.yongjibus.auth.email;

import lombok.RequiredArgsConstructor;

import java.util.Base64;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/gmail/bounce")
@RequiredArgsConstructor
class GmailBounceController {

    private final EmailBounceService emailBounceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/push-notifications")
    public void handlePushNotification(@RequestBody Map<String, Object> payload) {
        try {
            Map<String, Object> message = (Map<String, Object>) payload.get("message");
            String data = (String) message.get("data");
            String decodedData = new String(Base64.getDecoder().decode(data));

            Map<String, String> notificationData = objectMapper.readValue(decodedData, Map.class);
            String historyId = notificationData.get("historyId");

            if (historyId != null) {
                emailBounceService.processBounceNotification(historyId);
            }
        } catch (Exception e) {
            // 로깅
        }
    }
}