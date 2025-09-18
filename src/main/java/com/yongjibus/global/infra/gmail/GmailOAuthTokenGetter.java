package com.yongjibus.global.infra.gmail;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.GmailScopes;

import java.io.InputStreamReader;
import java.util.Collections;

public class GmailOAuthTokenGetter {
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    public static void main(String[] args) throws Exception {
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        var jsonFactory = GsonFactory.getDefaultInstance();

        var clientSecrets = GoogleClientSecrets.load(
                jsonFactory,
                new InputStreamReader(GmailOAuthTokenGetter.class.getResourceAsStream(CREDENTIALS_FILE_PATH))
        );

        var flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                jsonFactory,
                clientSecrets,
                Collections.singletonList(GmailScopes.GMAIL_READONLY)
        ).setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
         .setAccessType("offline")
         .setApprovalPrompt("force")
         .build();

        var receiver = new com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver.Builder().setPort(8888).build();
        var credential = new com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

        System.out.println("Access Token: " + credential.getAccessToken());
        System.out.println("Refresh Token: " + credential.getRefreshToken());
    }
}