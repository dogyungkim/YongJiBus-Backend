package com.yongjibus.global.infra.gmail;

import java.math.BigInteger;
import java.io.IOException;
import java.util.List;
import java.security.GeneralSecurityException;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListHistoryResponse;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.WatchResponse;
import com.google.api.services.gmail.model.WatchRequest;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GmailApiService {
    private static final String USER_ID = "me";
    private static final String APPLICATION_NAME = "YongJiBus";

    private final GmailProperties gmailProperties;
    private volatile Gmail gmailService;

    public GmailApiService(GmailProperties gmailProperties) {
        this.gmailProperties = gmailProperties;
    }

    public boolean isConfigured() {
        return gmailProperties.isEnabled()
                && StringUtils.hasText(gmailProperties.getOauth().getClientId())
                && StringUtils.hasText(gmailProperties.getOauth().getClientSecret())
                && StringUtils.hasText(gmailProperties.getOauth().getRefreshToken());
    }

    /**
     * History ID를 사용하여 메시지 추가 내역을 가져옵니다.
     */
    public ListHistoryResponse getHistory(BigInteger startHistoryId) throws IOException {
        return getHistory(startHistoryId, null);
    }

    public ListHistoryResponse getHistory(BigInteger startHistoryId, String pageToken) throws IOException {
        var request = getGmailService().users().history()
                .list(USER_ID)
                .setStartHistoryId(startHistoryId)
                .setHistoryTypes(List.of("messageAdded"));
        if (pageToken != null && !pageToken.isBlank()) {
            request.setPageToken(pageToken);
        }
        return request.execute();
    }

    public BigInteger getCurrentHistoryId() throws IOException {
        return getGmailService().users().getProfile(USER_ID).execute().getHistoryId();
    }

    public ListMessagesResponse getBounceMessages(String pageToken) throws IOException {
        var request = getGmailService().users().messages()
                .list(USER_ID)
                .setLabelIds(List.of(gmailProperties.getWatchedLabelId()));
        if (pageToken != null && !pageToken.isBlank()) {
            request.setPageToken(pageToken);
        }
        return request.execute();
    }

    /**
     * 메시지 ID를 사용하여 메시지를 가져옵니다.
     */
    public Message getMessage(String messageId) throws IOException {
        Message message = getGmailService().users().messages()
                .get(USER_ID, messageId)
                .setFormat("metadata")
                .execute();
        return message;
    }

    /**
     * Gmail 받은 편지함에 대한 Watch를 설정하거나 갱신합니다.
     * 바운스 메일 감지를 위해 Gmail API의 watch 기능을 사용합니다.
     */
    public BigInteger watchBounceMailBox() throws IOException {
        if (!isConfigured()) {
            log.info("Skipping Gmail watch registration because Gmail OAuth settings are disabled or incomplete");
            return null;
        }

        WatchRequest watchRequest = new WatchRequest()
            .setLabelIds(List.of(gmailProperties.getWatchedLabelId()))
            .set("labelFilterBehavior", "include")
            .setTopicName(gmailProperties.getTopicName());

       WatchResponse watchResponse = getGmailService().users().watch(USER_ID, watchRequest).execute();
       log.info("Registered Gmail watch with historyId={} expiration={}",
               watchResponse.getHistoryId(), watchResponse.getExpiration());
       return watchResponse.getHistoryId();
    }

    /**
     * 사용자가 만든 Gmail Label을 확인합니다.
     */
    public void listLabels() throws IOException {
        ListLabelsResponse listLabelsResponse = getGmailService().users().labels().list(USER_ID).execute();
        log.info("List Labels Response: {}", listLabelsResponse);
    }

    private Gmail getGmailService() throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Gmail API is not configured");
        }

        Gmail existingService = gmailService;
        if (existingService != null) {
            return existingService;
        }

        synchronized (this) {
            if (gmailService == null) {
                gmailService = buildGmailService();
            }
            return gmailService;
        }
    }

    private Gmail buildGmailService() throws IOException {
        try {
            var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            var jsonFactory = GsonFactory.getDefaultInstance();

            GoogleCredential credential = new GoogleCredential.Builder()
                    .setTransport(httpTransport)
                    .setJsonFactory(jsonFactory)
                    .setClientSecrets(
                            gmailProperties.getOauth().getClientId(),
                            gmailProperties.getOauth().getClientSecret())
                    .build()
                    .setRefreshToken(gmailProperties.getOauth().getRefreshToken());

            if (!credential.refreshToken()) {
                throw new IOException("Failed to refresh Gmail access token");
            }

            return new Gmail.Builder(httpTransport, jsonFactory, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to initialize Gmail API client", e);
        }
    }
    
}   
