package com.creatoros.publishing.services;

import com.creatoros.publishing.entities.ConnectedAccount;
import com.creatoros.publishing.repositories.ConnectedAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LinkedInOAuthService {

    private final ConnectedAccountRepository accountRepository;
    private final RestTemplate restTemplate;

    @Value("${linkedin.client-id}")
    private String clientId;

    @Value("${linkedin.client-secret}")
    private String clientSecret;

    @Value("${linkedin.redirect-uri}")
    private String redirectUri;

    @Value("${linkedin.token-url}")
    private String tokenUrl;

    @Value("${linkedin.api-base-url}")
    private String apiBaseUrl;

    public String buildAuthorizationUrl() {
        return "https://www.linkedin.com/oauth/v2/authorization"
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&scope=w_member_social";
    }

    public void handleCallback(String code) {
        // 1️⃣ Exchange code → access token
        @SuppressWarnings("unchecked")
        Map<String, Object> tokenResponse = restTemplate.postForObject(
                tokenUrl
                        + "?grant_type=authorization_code"
                        + "&code=" + code
                        + "&redirect_uri=" + redirectUri
                        + "&client_id=" + clientId
                        + "&client_secret=" + clientSecret,
                null,
                Map.class
        );

        String accessToken = (String) tokenResponse.get("access_token");

        // 2️⃣ Fetch author URN
        @SuppressWarnings("unchecked")
        Map<String, Object> profile = restTemplate.getForObject(
                apiBaseUrl + "/me?oauth2_access_token=" + accessToken,
                Map.class
        );

        String authorUrn = "urn:li:person:" + profile.get("id");

        // 3️⃣ Save connected account
        ConnectedAccount account = ConnectedAccount.builder()
                .userId(mockUserId())
                .platform("LINKEDIN")
                .accountType("PERSONAL")
                .accountName("LinkedIn Account")
                .platformAccountId((String) profile.get("id"))
                .linkedinAuthorUrn(authorUrn)
                .accessTokenEnc(accessToken)
                .isActive(true)
                .connectedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        accountRepository.save(account);
    }

    private UUID mockUserId() {
        return UUID.fromString("11111111-1111-1111-1111-111111111111");
    }
}
