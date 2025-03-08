package com.yongjibus.global.config.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class FireBaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            GoogleCredentials googleCredentials = GoogleCredentials
                    .fromStream(new ClassPathResource("firebase/yongjibus-firebase-adminsdk-i5oxw-6c26cc2dfb.json").getInputStream());
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(googleCredentials)
                    .build();
            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }
} 