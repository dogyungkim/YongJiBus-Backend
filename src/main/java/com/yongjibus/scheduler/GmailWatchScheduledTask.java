package com.yongjibus.scheduler;

import java.io.IOException;
import java.math.BigInteger;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.yongjibus.auth.email.EmailBounceService;
import com.yongjibus.global.infra.gmail.GmailApiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class GmailWatchScheduledTask {
    private final GmailApiService gmailApiService;
    private final EmailBounceService emailBounceService;

    @EventListener(ApplicationReadyEvent.class)
    public void registerOnStartup() {
        refreshWatch("startup");
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void execute() {
        refreshWatch("scheduled");
    }

    private void refreshWatch(String trigger) {
        if (!gmailApiService.isConfigured()) {
            log.info("Skipping Gmail watch refresh on {} because Gmail OAuth settings are disabled or incomplete", trigger);
            return;
        }

        try {
            BigInteger watchHistoryId = gmailApiService.watchBounceMailBox();
            if (watchHistoryId != null) {
                emailBounceService.initializeCheckpointIfAbsent(watchHistoryId);
            }
        } catch (IOException e) {
            log.error("Failed to refresh Gmail watch on {}", trigger, e);
        }
    }
}
