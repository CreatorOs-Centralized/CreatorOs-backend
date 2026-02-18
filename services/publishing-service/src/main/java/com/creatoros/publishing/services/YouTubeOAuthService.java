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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
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

    @Value("${youtube.state-secret}")
    private String stateSecret;

    @Value("${youtube.state-ttl-seconds:600}")
    private long stateTtlSeconds;

    /**
     * Build YouTube OAuth authorization URL
     * Scopes: youtube.upload (upload videos), youtube.readonly (read channel info), yt-analytics.readonly (analytics)
     */
    public String buildAuthorizationUrl(String userId) {
        String state = buildState(userId);
        return authUrl
                + "?client_id=" + urlEncode(clientId)
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&response_type=code"
                + "&scope=" + urlEncode("https://www.googleapis.com/auth/youtube.upload https://www.googleapis.com/auth/youtube.readonly https://www.googleapis.com/auth/yt-analytics.readonly")
                + "&access_type=offline"  // Critical: ensures refresh token is returned
                + "&prompt=consent"       // Forces consent screen to get refresh token
                + "&state=" + urlEncode(state);
    }

    /**
     * Handle OAuth callback and exchange code for tokens
     */
    public void handleCallback(String userId, String code) {
        
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
                .userId(UUID.fromString(userId))
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

    public String resolveState(String state) {
        if (state == null || state.isBlank()) {
            throw new RuntimeException("Missing OAuth state");
        }

        String[] parts = state.split("\\.");
        if (parts.length != 2) {
            throw new RuntimeException("Invalid OAuth state format");
        }

        String payload = new String(base64UrlDecode(parts[0]), StandardCharsets.UTF_8);
        String expectedSignature = hmacSha256Base64Url(payload, stateSecret);
        if (!MessageDigest.isEqual(base64UrlDecode(parts[1]), base64UrlDecode(expectedSignature))) {
            throw new RuntimeException("Invalid OAuth state signature");
        }

        String[] payloadParts = payload.split(":", 2);
        if (payloadParts.length != 2) {
            throw new RuntimeException("Invalid OAuth state payload");
        }

        String userId = payloadParts[0];
        long issuedAt = parseEpochSeconds(payloadParts[1]);
        long now = Instant.now().getEpochSecond();
        if (stateTtlSeconds > 0 && now - issuedAt > stateTtlSeconds) {
            throw new RuntimeException("OAuth state expired");
        }

        return userId;
    }

    private String buildState(String userId) {
        if (stateSecret == null || stateSecret.isBlank()) {
            throw new RuntimeException("Missing youtube.state-secret configuration");
        }
        if (userId == null || userId.isBlank()) {
            throw new RuntimeException("Missing userId for OAuth state");
        }

        long issuedAt = Instant.now().getEpochSecond();
        String payload = userId + ":" + issuedAt;
        String signature = hmacSha256Base64Url(payload, stateSecret);
        return base64UrlEncode(payload) + "." + signature;
    }

    private static String hmacSha256Base64Url(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return base64UrlEncode(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to compute OAuth state signature", ex);
        }
    }

    private static long parseEpochSeconds(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new RuntimeException("Invalid OAuth state timestamp");
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String base64UrlEncode(String value) {
        return base64UrlEncode(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String base64UrlEncode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
