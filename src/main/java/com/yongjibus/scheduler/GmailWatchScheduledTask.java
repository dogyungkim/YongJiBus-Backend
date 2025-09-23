package com.yongjibus.scheduler;

import java.io.IOException;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.yongjibus.global.infra.gmail.GmailApiService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GmailWatchScheduledTask {
    private final GmailApiService gmailApiService;

    @Scheduled(cron = "0 0 4 * * *")
    public void execute() throws IOException { 
        gmailApiService.watchBounceMailBox();
    }
}
