package com.creatoros.publishing.services;

import com.creatoros.publishing.entities.ConnectedAccount;
import com.creatoros.publishing.repositories.ConnectedAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstagramOAuthService {

    private final ConnectedAccountRepository accountRepository;
    private final RestTemplate restTemplate;

    @Value("${instagram.client-id}")
    private String clientId;

    @Value("${instagram.client-secret}")
    private String clientSecret;

    @Value("${instagram.redirect-uri}")
    private String redirectUri;

    @Value("${instagram.token-url}")
    private String tokenUrl;

    @Value("${instagram.api-base-url}")
    private String apiBaseUrl;

    /**
     * Build Instagram OAuth authorization URL
     * Scopes: instagram_business_basic, instagram_business_content_publish
     */
    public String buildAuthorizationUrl(String userId) {
        return "https://api.instagram.com/oauth/authorize"
                + "?client_id=" + urlEncode(clientId)
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&scope=" + urlEncode("instagram_business_basic,instagram_business_content_publish,pages_read_engagement,pages_manage_posts")
                + "&response_type=code";
    }

    /**
     * Handle OAuth callback and exchange code for access token
     */
    public void handleCallback(String userId, String code) {
        log.info("Handling Instagram OAuth callback for user: {}", userId);

        try {
            // Step 1: Exchange authorization code for access token
            HttpHeaders tokenHeaders = new HttpHeaders();
            tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("grant_type", "authorization_code");
            body.add("redirect_uri", redirectUri);
            body.add("code", code);

            HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(body, tokenHeaders);

            ResponseEntity<Map> tokenResponse = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    tokenRequest,
                    Map.class
            );

            Map<String, Object> tokenData = tokenResponse.getBody();
            if (tokenData == null) {
                throw new RuntimeException("Empty response from Instagram token exchange");
            }

            String accessToken = (String) tokenData.get("access_token");
            String instagramBusinessAccountId = (String) tokenData.get("user_id");

            if (accessToken == null) {
                throw new RuntimeException("No access token in Instagram response");
            }

            log.info("Successfully obtained Instagram access token for user: {}", instagramBusinessAccountId);

            // Step 2: Fetch Instagram business account info
            HttpHeaders profileHeaders = new HttpHeaders();
            profileHeaders.setBearerAuth(accessToken);
            profileHeaders.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<String> profileRequest = new HttpEntity<>(profileHeaders);

            ResponseEntity<Map> profileResponse = restTemplate.exchange(
                    apiBaseUrl + "/me?fields=id,username,name,biography",
                    HttpMethod.GET,
                    profileRequest,
                    Map.class
            );

            Map<String, Object> profileData = profileResponse.getBody();
            if (profileData == null) {
                throw new RuntimeException("No profile information returned from Instagram");
            }

            String instagramId = (String) profileData.get("id");
            String username = (String) profileData.get("username");
            String name = (String) profileData.get("name");

            log.info("Found Instagram business account: {} (@{})", name, username);

            // Step 3: Save connected account
            ConnectedAccount account = ConnectedAccount.builder()
                    .userId(UUID.fromString(userId))
                    .platform("INSTAGRAM")
                    .accountType("BUSINESS")
                    .accountName(name != null ? name : username)
                    .platformAccountId(instagramId)
                    .instagramBusinessAccountId(instagramBusinessAccountId)
                    .accessTokenEnc(accessToken)
                    .scopes("instagram_business_basic,instagram_business_content_publish,pages_read_engagement,pages_manage_posts")
                    .isActive(true)
                    .connectedAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            accountRepository.save(account);

            log.info("Successfully connected Instagram business account: {}", instagramId);
        } catch (Exception ex) {
            log.error("Failed to handle Instagram OAuth callback", ex);
            throw new RuntimeException("Instagram OAuth callback failed: " + ex.getMessage(), ex);
        }
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
