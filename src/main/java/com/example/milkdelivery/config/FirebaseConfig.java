package com.example.milkdelivery.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.config.path:}")
    private String firebaseConfigPath;

    private boolean firebaseInitialized = false;

    @PostConstruct
    public void initializeFirebase() {
        if (firebaseConfigPath == null || firebaseConfigPath.trim().isEmpty()) {
            logger.warn("[FIREBASE] Config path is empty. Firebase Admin SDK will not be initialized. Real tokens will fail verification, falling back to mock authentication.");
            return;
        }

        File configFile = new File(firebaseConfigPath);
        if (!configFile.exists()) {
            logger.warn("[FIREBASE] Configuration file not found at path: {}. Firebase Admin SDK will not be initialized. Real tokens will fail verification, falling back to mock authentication.", firebaseConfigPath);
            return;
        }

        try (FileInputStream serviceAccount = new FileInputStream(configFile)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                logger.info("[FIREBASE] Firebase Admin SDK initialized successfully from file: {}", firebaseConfigPath);
            }
            firebaseInitialized = true;
        } catch (IOException e) {
            logger.error("[FIREBASE] Error initializing Firebase Admin SDK: {}", e.getMessage(), e);
        }
    }

    public boolean isFirebaseInitialized() {
        return firebaseInitialized;
    }
}
