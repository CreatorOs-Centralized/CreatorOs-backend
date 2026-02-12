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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class YouTubeOAuthService {

    private final ConnectedAccountRepository accountRepository;
    private final RestTemplate restTemplate;

    @Value("${youtube.client-id}")
    private String clientId;

    @Value("${youtube.client-secret}")
    private String clientSecret;

    @Value("${youtube.redirect-uri}")
    private String redirectUri;

    @Value("${youtube.token-url}")
    private String tokenUrl;

    @Value("${youtube.auth-url}")
    private String authUrl;

    /**
     * Build YouTube OAuth authorization URL
     * Scopes: youtube.upload (upload videos), youtube.readonly (read channel info), yt-analytics.readonly (analytics)
     */
    public String buildAuthorizationUrl() {
        return authUrl
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=https://www.googleapis.com/auth/youtube.upload+https://www.googleapis.com/auth/youtube.readonly+https://www.googleapis.com/auth/yt-analytics.readonly"
                + "&access_type=offline"  // Critical: ensures refresh token is returned
                + "&prompt=consent"       // Forces consent screen to get refresh token
                + "&state=random_state_value";
    }

    /**
     * Handle OAuth callback and exchange code for tokens
     */
    public void handleCallback(String code) {
        
        log.info("Handling YouTube OAuth callback");

        // Step 1: Exchange authorization code for access token + refresh token
        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(body, tokenHeaders);

        ResponseEntity<Map> tokenResponse = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                tokenRequest,
                Map.class
        );

        Map<String, Object> tokenData = tokenResponse.getBody();
        if (tokenData == null) {
            throw new RuntimeException("Empty response from token exchange");
        }

        String accessToken = (String) tokenData.get("access_token");
        String refreshToken = (String) tokenData.get("refresh_token");
        Integer expiresIn = (Integer) tokenData.get("expires_in");

        if (accessToken == null) {
            throw new RuntimeException("No access token in response");
        }

        if (refreshToken == null) {
            log.warn("No refresh token returned - user may have already authenticated before");
            throw new RuntimeException("No refresh token returned. Please revoke access and try again.");
        }

        log.info("Successfully obtained access token and refresh token");

        // Step 2: Fetch YouTube channel information
        HttpHeaders channelHeaders = new HttpHeaders();
        channelHeaders.setBearerAuth(accessToken);
        channelHeaders.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> channelRequest = new HttpEntity<>(channelHeaders);

        ResponseEntity<Map> channelResponse = restTemplate.exchange(
                "https://www.googleapis.com/youtube/v3/channels?part=snippet,contentDetails&mine=true",
                HttpMethod.GET,
                channelRequest,
                Map.class
        );

        Map<String, Object> channelData = channelResponse.getBody();
        if (channelData == null || !channelData.containsKey("items")) {
            throw new RuntimeException("No channel information returned");
        }

        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) channelData.get("items");
        
        if (items.isEmpty()) {
            throw new RuntimeException("No YouTube channel found for this account");
        }

        Map<String, Object> channel = items.get(0);
        String channelId = (String) channel.get("id");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> snippet = (Map<String, Object>) channel.get("snippet");
        String channelTitle = (String) snippet.get("title");

        log.info("Found YouTube channel: {} ({})", channelTitle, channelId);

        // Step 3: Save connected account
        ConnectedAccount account = ConnectedAccount.builder()
                .userId(mockUserId())  // TODO: Get from JWT
                .platform("YOUTUBE")
                .accountType("CHANNEL")
                .accountName(channelTitle)
                .platformAccountId(channelId)
                .youtubeChannelId(channelId)
                .accessTokenEnc(accessToken)
                .refreshTokenEnc(refreshToken)
                .tokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn != null ? expiresIn : 3600))
                .scopes("youtube.upload,youtube.readonly,yt-analytics.readonly")
                .isActive(true)
                .connectedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        accountRepository.save(account);
        
        log.info("Successfully connected YouTube channel: {}", channelId);
    }

    private UUID mockUserId() {
        return UUID.fromString("11111111-1111-1111-1111-111111111111");
    }
}
