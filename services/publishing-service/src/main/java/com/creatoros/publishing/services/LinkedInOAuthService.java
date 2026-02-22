package com.creatoros.publishing.services;

import com.creatoros.publishing.entities.ConnectedAccount;
import com.creatoros.publishing.repositories.ConnectedAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

    /**
     * STEP 1 — Build Authorization URL
     */
    public String buildAuthorizationUrl() {
        return "https://www.linkedin.com/oauth/v2/authorization"
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&scope=openid%20profile%20email%20w_member_social&state=random_state_value";
    }

    /**
     * STEP 2 — Handle OAuth Callback
     */
    public void handleCallback(String userId, String code) {

        // Exchange authorization code for access token
        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> tokenRequest =
                new HttpEntity<>(body, tokenHeaders);

        ResponseEntity<Map> tokenResponse = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                tokenRequest,
                Map.class
        );

        String accessToken = (String) tokenResponse.getBody().get("access_token");

        // Fetch LinkedIn profile using OpenID Connect userinfo endpoint
        HttpHeaders profileHeaders = new HttpHeaders();
        profileHeaders.setBearerAuth(accessToken);
        profileHeaders.set("Accept", "application/json");
        profileHeaders.set("Accept-Language", "en-US");

        HttpEntity<String> entity = new HttpEntity<>(profileHeaders);

        // Use OpenID Connect endpoint instead of REST API
        ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.linkedin.com/v2/userinfo",
                HttpMethod.GET,
                entity,
                Map.class
        );

        Map<String, Object> profile = response.getBody();
        String linkedinId = (String) profile.get("sub");  // OpenID Connect uses 'sub' for user ID
        String email = (String) profile.get("email");
        String name = (String) profile.get("name");
        String authorUrn = linkedinId;  // userinfo already returns the URN

        // Save connected account
        ConnectedAccount account = ConnectedAccount.builder()
                .userId(UUID.fromString(userId))
                .platform("LINKEDIN")
                .accountType("PERSONAL")
                .accountName(name != null ? name : "LinkedIn Account")
                .platformAccountId(linkedinId)
                .linkedinAuthorUrn(authorUrn)
                .accessTokenEnc(accessToken)
                .isActive(true)
                .connectedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        accountRepository.save(account);
    }

}
