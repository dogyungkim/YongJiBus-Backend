package com.yongjibus.global.infra.gmail;

import java.io.InputStreamReader;
import java.math.BigInteger;
import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListHistoryResponse;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.WatchResponse;

import lombok.extern.slf4j.Slf4j;

import com.google.api.services.gmail.model.WatchRequest;

@Service
@Slf4j
public class GmailApiService {
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String TOPIC_NAME = "projects/yongji-bus/topics/auth-mail-failure";
    private static final String USER_ID = "me";

    private final Gmail gmailService;
    
    public GmailApiService() throws Exception {
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        var jsonFactory = GsonFactory.getDefaultInstance();

        var clientSecrets = GoogleClientSecrets.load(
                jsonFactory,
                new InputStreamReader(GmailApiService.class.getResourceAsStream(CREDENTIALS_FILE_PATH))
        );

        var flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                jsonFactory,
                clientSecrets,
                List.of(GmailScopes.GMAIL_READONLY, GmailScopes.GMAIL_MODIFY)
        ).setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
         .setAccessType("offline")
         .setApprovalPrompt("force")
         .build();

        var receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        var credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

        this.gmailService = new Gmail.Builder(httpTransport, jsonFactory, credential).build();
        this.watchBounceMailBox();
    }

    /**
     * History ID를 사용하여 메시지 추가 내역을 가져옵니다.
     */
    public ListHistoryResponse getHistory(BigInteger startHistoryId) throws IOException {
        ListHistoryResponse history = gmailService.users().history()
                .list(USER_ID)
                .setStartHistoryId(startHistoryId)
                .setHistoryTypes(List.of("messageAdded"))
                .execute();
        return history;
    }

    /**
     * 메시지 ID를 사용하여 메시지를 가져옵니다.
     */
    public Message getMessage(String messageId) throws IOException {
        Message message = gmailService.users().messages()
                .get(USER_ID, messageId)
                .setFormat("metadata")
                .execute();
        return message;
    }

    /**
     * Gmail 받은 편지함에 대한 Watch를 설정하거나 갱신합니다.
     * 바운스 메일 감지를 위해 Gmail API의 watch 기능을 사용합니다.
     */
    public void watchBounceMailBox() throws IOException {
        WatchRequest watchRequest = new WatchRequest()
            .setLabelIds(List.of("Label_6"))
            .setLabelFilterAction("include")
            .setTopicName(TOPIC_NAME);

       WatchResponse watchResponse = gmailService.users().watch(USER_ID, watchRequest).execute();
       log.debug("Watch Response: {}", watchResponse);
    }

    /**
     * 사용자가 만든 Gmail Label을 확인합니다.
     */
    public void listLabels() throws IOException {
        ListLabelsResponse listLabelsResponse = gmailService.users().labels().list(USER_ID).execute();
        log.info("List Labels Response: {}", listLabelsResponse);
    }
    
}   