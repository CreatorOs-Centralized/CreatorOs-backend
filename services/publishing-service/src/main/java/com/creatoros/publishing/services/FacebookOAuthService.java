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
public class FacebookOAuthService {

    private final ConnectedAccountRepository accountRepository;
    private final RestTemplate restTemplate;

    @Value("${facebook.client-id}")
    private String clientId;

    @Value("${facebook.client-secret}")
    private String clientSecret;

    @Value("${facebook.redirect-uri}")
    private String redirectUri;

    @Value("${facebook.token-url}")
    private String tokenUrl;

    @Value("${facebook.api-base-url}")
    private String apiBaseUrl;

    /**
     * Build Facebook OAuth authorization URL
     * Scopes: pages_read_engagement, pages_manage_posts, pages_manage_metadata, publish_pages
     */
    public String buildAuthorizationUrl(String userId) {
        return "https://www.facebook.com/v18.0/dialog/oauth"
                + "?client_id=" + urlEncode(clientId)
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&scope=" + urlEncode("pages_read_engagement,pages_manage_posts,pages_manage_metadata,publish_pages")
                + "&response_type=code";
    }

    /**
     * Handle OAuth callback and exchange code for access token
     */
    public void handleCallback(String userId, String code) {
        log.info("Handling Facebook OAuth callback for user: {}", userId);

        try {
            // Step 1: Exchange authorization code for access token
            HttpHeaders tokenHeaders = new HttpHeaders();
            tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
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
                throw new RuntimeException("Empty response from Facebook token exchange");
            }

            String accessToken = (String) tokenData.get("access_token");

            if (accessToken == null) {
                throw new RuntimeException("No access token in Facebook response");
            }

            log.info("Successfully obtained Facebook access token");

            // Step 2: Fetch Facebook user info
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            userHeaders.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<String> userRequest = new HttpEntity<>(userHeaders);

            ResponseEntity<Map> userResponse = restTemplate.exchange(
                    apiBaseUrl + "/me?fields=id,name,email",
                    HttpMethod.GET,
                    userRequest,
                    Map.class
            );

            Map<String, Object> userData = userResponse.getBody();
            if (userData == null) {
                throw new RuntimeException("No user information returned from Facebook");
            }

            String facebookUserId = (String) userData.get("id");
            String name = (String) userData.get("name");
            String email = (String) userData.get("email");

            // Step 3: Fetch Facebook pages
            ResponseEntity<Map> pagesResponse = restTemplate.exchange(
                    apiBaseUrl + "/me/accounts",
                    HttpMethod.GET,
                    userRequest,
                    Map.class
            );

            Map<String, Object> pagesData = pagesResponse.getBody();
            String pageId = null;
            String pageName = null;
            String pageAccessToken = null;

            if (pagesData != null && pagesData.containsKey("data")) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> pages = (java.util.List<Map<String, Object>>) pagesData.get("data");
                if (!pages.isEmpty()) {
                    Map<String, Object> firstPage = pages.get(0);
                    pageId = (String) firstPage.get("id");
                    pageName = (String) firstPage.get("name");
                    pageAccessToken = (String) firstPage.get("access_token");
                }
            }

            if (pageId == null) {
                log.warn("No Facebook pages found for user: {}", facebookUserId);
            }

            log.info("Found Facebook user: {} with page: {}", name, pageName);

            // Step 4: Save connected account
            ConnectedAccount account = ConnectedAccount.builder()
                    .userId(UUID.fromString(userId))
                    .platform("FACEBOOK")
                    .accountType("PAGE")
                    .accountName(pageName != null ? pageName : name)
                    .platformAccountId(facebookUserId)
                    .facebookPageId(pageId)
                    .accessTokenEnc(accessToken)
                    .scopes("pages_read_engagement,pages_manage_posts,pages_manage_metadata,publish_pages")
                    .isActive(true)
                    .connectedAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            accountRepository.save(account);

            log.info("Successfully connected Facebook page: {} (ID: {})", pageName, pageId);
        } catch (Exception ex) {
            log.error("Failed to handle Facebook OAuth callback", ex);
            throw new RuntimeException("Facebook OAuth callback failed: " + ex.getMessage(), ex);
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
