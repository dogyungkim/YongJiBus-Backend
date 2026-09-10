package com.yongjibus.auth.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.management.RuntimeErrorException;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.AuthException;
import com.yongjibus.global.common.response.YongJiResponse;
import com.yongjibus.global.infra.gmail.GmailPubSubTokenVerifier;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/gmail/bounce")
@RequiredArgsConstructor
@Slf4j
class GmailBounceController {

    private final EmailBounceService emailBounceService;
    private final ObjectMapper objectMapper;
    private final GmailPubSubTokenVerifier tokenVerifier;


    @PostMapping(consumes = { "application/json", "application/octet-stream" })
    public ResponseEntity<YongJiResponse<Void>> post(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody byte[] body) throws JsonMappingException, JsonProcessingException {
        if (!tokenVerifier.isValid(authorizationHeader)) {
            throw new AuthException(ErrorCode.UNAUTHORIZED);
        }

        // application/octet-stream 이라도 본문은 JSON 문자열일 가능성이 높음
        var json = new String(body, java.nio.charset.StandardCharsets.UTF_8);
        try {
            var root = objectMapper.readTree(json);
            var base64Data = root.path("message").path("data").asText(null);
            if (base64Data == null || base64Data.isEmpty()) {
                log.warn("Invalid notification: missing message.data");
                return YongJiResponse.success(null);
            }

            // pub/sub 메시지 본문 디코딩
            var decoded = new String(java.util.Base64.getDecoder().decode(base64Data), java.nio.charset.StandardCharsets.UTF_8);
            var inner = objectMapper.readTree(decoded);

            // HistoryId may be number or string; asText() handles both
            var historyId = inner.path("historyId").asText(null);
            
            log.info("HistoryId: {}", historyId);
            if (historyId == null || historyId.isEmpty()) {
                //Bounce 관련이 아니여서 ok 처리, 메시지 재시도 방지
                log.warn("Invalid notification: missing HistoryId in decoded payload: {}", decoded);
                return YongJiResponse.success(null);
            }

            try {
                emailBounceService.processBounceNotification(historyId);
            } catch (Exception e) {
                log.error("Error processing Gmail bounce notification", e);
                throw new AuthException(ErrorCode.INVALID_REQUEST);
            }

            return YongJiResponse.success(null);
        } catch (Exception e) {
            log.error("Failed to parse Gmail bounce notification", e);
            throw new AuthException(ErrorCode.INVALID_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<YongJiResponse<Boolean>> getBounceMail(@RequestParam String emailAddress) {
        return YongJiResponse.success(emailBounceService.isEmailPending(emailAddress));
    }
    
}
