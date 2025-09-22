package com.yongjibus.auth.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/gmail/bounce")
@RequiredArgsConstructor
@Slf4j
class GmailBounceController {

    private final EmailBounceService emailBounceService;

    @PostMapping("/push-notification")
    public ResponseEntity<Void> handlePushNotification(@RequestBody GmailBounceMessageDTO dto) {
        try {
            log.info("Received Gmail bounce notification: {}", dto);
            
            String historyId = dto.message().data().historyId();   
        
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