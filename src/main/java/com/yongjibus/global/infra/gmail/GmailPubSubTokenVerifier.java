package com.yongjibus.global.infra.gmail;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@Component
public class GmailPubSubTokenVerifier {
    private final GmailProperties gmailProperties;
    private volatile GoogleIdTokenVerifier verifier;

    public GmailPubSubTokenVerifier(GmailProperties gmailProperties) {
        this.gmailProperties = gmailProperties;
    }

    public boolean isValid(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return false;
        }

        String token = authorizationHeader.substring("Bearer ".length());
        String audience = gmailProperties.getPubsubAudience();
        String serviceAccountEmail = gmailProperties.getPubsubServiceAccountEmail();
        if (token.isBlank() || token.indexOf(' ') >= 0 || token.indexOf('\t') >= 0
                || !StringUtils.hasText(audience) || !StringUtils.hasText(serviceAccountEmail)) {
            return false;
        }

        try {
            GoogleIdToken idToken = getVerifier(audience).verify(token);
            if (idToken == null) {
                return false;
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            return payload != null
                    && Boolean.TRUE.equals(payload.getEmailVerified())
                    && serviceAccountEmail.equals(payload.getEmail());
        } catch (GeneralSecurityException | IOException | RuntimeException e) {
            return false;
        }
    }

    private GoogleIdTokenVerifier getVerifier(String audience) throws GeneralSecurityException, IOException {
        GoogleIdTokenVerifier existingVerifier = verifier;
        if (existingVerifier != null) {
            return existingVerifier;
        }

        synchronized (this) {
            if (verifier == null) {
                verifier = new GoogleIdTokenVerifier.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance())
                        .setAudience(List.of(audience))
                        .build();
            }
            return verifier;
        }
    }
}
