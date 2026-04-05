package com.kaitohuy.chiabill.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleTokenService {

    @Value("${google.client-id}")
    private String clientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        try {
            var transport = GoogleNetHttpTransport.newTrustedTransport();
            this.verifier = new GoogleIdTokenVerifier.Builder(
                    transport,
                    JacksonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(clientId))
                    // Bổ sung Acceptable Time Skew (24 tiếng) để tránh lỗi lệch Timezone giữa máy tính nội bộ của bạn và Google Server
                    .setAcceptableTimeSkewSeconds(86400)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize GoogleIdTokenVerifier", e);
        }
    }

    public GoogleIdToken.Payload verify(String idTokenString) {
        try {
            System.out.println("\n========== GOOGLE TOKEN DEBUG ==========");
            System.out.println("Configured Google Client ID: " + clientId);
            
            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                System.out.println("Verifier returned null! Token might be expired, wrong audience, or invalid signature.");
                try {
                    GoogleIdToken parsed = GoogleIdToken.parse(verifier.getJsonFactory(), idTokenString);
                    if (parsed != null) {
                        System.out.println("Parsed Audience: " + parsed.getPayload().getAudience());
                        System.out.println("Parsed Issuer: " + parsed.getPayload().getIssuer());
                        System.out.println("Parsed Expiration Time: " + parsed.getPayload().getExpirationTimeSeconds());
                        System.out.println("Current Time: " + (System.currentTimeMillis() / 1000));
                    } else {
                        System.out.println("Failed to parse token even without verifying!");
                    }
                } catch (Exception parseEx) {
                    System.out.println("Exception parsing token: " + parseEx.getMessage());
                }
                System.out.println("========================================\n");
                throw new RuntimeException("Invalid Google token");
            }
            System.out.println("Token verification SUCCESS!");
            System.out.println("========================================\n");

            return idToken.getPayload();

        } catch (Exception e) {
            throw new RuntimeException("Invalid Google token", e);
        }
    }
}