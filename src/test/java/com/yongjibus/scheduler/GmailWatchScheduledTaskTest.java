package com.yongjibus.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yongjibus.auth.email.EmailBounceService;
import com.yongjibus.global.infra.gmail.GmailApiService;

@ExtendWith(MockitoExtension.class)
class GmailWatchScheduledTaskTest {

    @Mock
    private GmailApiService gmailApiService;

    @Mock
    private EmailBounceService emailBounceService;

    @InjectMocks
    private GmailWatchScheduledTask gmailWatchScheduledTask;

    @Test
    @DisplayName("watch 등록 성공 시 checkpoint가 없을 때만 초기 기준점을 저장한다")
    void registerOnStartup_ShouldInitializeCheckpoint() throws IOException {
        // given
        when(gmailApiService.isConfigured()).thenReturn(true);
        when(gmailApiService.watchBounceMailBox()).thenReturn(BigInteger.valueOf(200));

        // when
        gmailWatchScheduledTask.registerOnStartup();

        // then
        verify(emailBounceService).initializeCheckpointIfAbsent(BigInteger.valueOf(200));
    }

    @Test
    @DisplayName("Gmail 설정이 비활성화되어 있으면 watch 등록과 checkpoint 저장을 모두 건너뛴다")
    void registerOnStartup_WhenDisabled_ShouldSkip() throws IOException {
        // given
        when(gmailApiService.isConfigured()).thenReturn(false);

        // when
        gmailWatchScheduledTask.registerOnStartup();

        // then
        verify(gmailApiService, never()).watchBounceMailBox();
        verifyNoInteractions(emailBounceService);
    }
}
