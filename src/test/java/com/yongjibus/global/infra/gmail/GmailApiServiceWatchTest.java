package com.yongjibus.global.infra.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.WatchRequest;
import com.google.api.services.gmail.model.WatchResponse;

@ExtendWith(MockitoExtension.class)
class GmailApiServiceWatchTest {

    @Mock
    private Gmail gmail;

    @Mock
    private Gmail.Users users;

    @Mock
    private Gmail.Users.Watch watch;

    private GmailApiService gmailApiService;

    @BeforeEach
    void setUp() throws Exception {
        GmailProperties gmailProperties = new GmailProperties();
        gmailProperties.setEnabled(true);
        gmailProperties.getOauth().setClientId("client-id");
        gmailProperties.getOauth().setClientSecret("client-secret");
        gmailProperties.getOauth().setRefreshToken("refresh-token");
        gmailApiService = new GmailApiService(gmailProperties);
        ReflectionTestUtils.setField(gmailApiService, "gmailService", gmail);

        when(gmail.users()).thenReturn(users);
        when(users.watch(eq("me"), any(WatchRequest.class))).thenReturn(watch);
        when(watch.execute()).thenReturn(new WatchResponse().setHistoryId(BigInteger.valueOf(200)));
    }

    @Test
    @DisplayName("watch 요청은 현재 labelFilterBehavior를 사용하고 deprecated 필드는 설정하지 않는다")
    void watchBounceMailBox_ShouldUseLabelFilterBehavior() throws Exception {
        assertThat(gmailApiService.watchBounceMailBox()).isEqualTo(BigInteger.valueOf(200));

        ArgumentCaptor<WatchRequest> requestCaptor = ArgumentCaptor.forClass(WatchRequest.class);
        verify(users).watch(eq("me"), requestCaptor.capture());
        WatchRequest request = requestCaptor.getValue();
        assertThat(request.get("labelFilterBehavior")).isEqualTo("include");
        assertThat(request.getLabelFilterAction()).isNull();
    }
}
