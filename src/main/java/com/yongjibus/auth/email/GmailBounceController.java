package com.yongjibus.auth.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/gmail/bounce")
@RequiredArgsConstructor
@Slf4j
class GmailBounceController {

    private final EmailBounceService emailBounceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/push-notification")
    public ResponseEntity<Void> handlePushNotification(@RequestBody GmailBounceMessageDTO dto) {
        try {
            log.info("Received Gmail bounce notification: {}", dto);
            
            String historyId = dto.message().historyId();   
        
            if (historyId != null) {
                log.info("Processing bounce notification for historyId: {}", historyId);
                emailBounceService.processBounceNotification(historyId);
                return ResponseEntity.ok().build();
            } else {
                log.warn("No historyId found in notification data");
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            log.error("Error processing Gmail bounce notification", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}