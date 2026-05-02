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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstagramOAuthService {

    private static final String AUTHORIZATION_URL = "https://www.instagram.com/oauth/authorize";
    private static final String REQUESTED_SCOPES = "instagram_business_basic,instagram_business_manage_messages,instagram_business_manage_comments,instagram_business_content_publish,instagram_business_manage_insights";

    private final ConnectedAccountRepository accountRepository;
    private final RestTemplate restTemplate;

    @Value("${instagram.client-id}")
    private String clientId;

    @Value("${instagram.client-secret}")
    private String clientSecret;

    @Value("${instagram.redirect-uri}")
    private String redirectUri;

    @Value("${instagram.token-url:https://api.instagram.com/oauth/access_token}")
    private String shortLivedTokenUrl;

    @Value("${instagram.long-lived-token-url:https://graph.instagram.com/access_token}")
    private String longLivedTokenUrl;

    @Value("${instagram.refresh-token-url:https://graph.instagram.com/refresh_access_token}")
    private String refreshTokenUrl;

    @Value("${instagram.api-base-url:https://graph.instagram.com/v25.0}")
    private String apiBaseUrl;

    @Value("${instagram.state-secret}")
    private String stateSecret;

    @Value("${instagram.state-ttl-seconds:600}")
    private long stateTtlSeconds;

    public String buildAuthorizationUrl(String userId) {
        String state = buildState(userId);
        return AUTHORIZATION_URL
                + "?client_id=" + urlEncode(clientId)
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&response_type=code"
                + "&scope=" + urlEncode(REQUESTED_SCOPES)
                + "&state=" + urlEncode(state);
    }

    public void handleCallback(String userId, String code) {
        log.info("Handling Instagram OAuth callback for user {}", userId);

        try {
            Map<String, Object> shortLivedTokenPayload = exchangeCodeForShortLivedToken(code);
            String shortLivedAccessToken = requiredValue(shortLivedTokenPayload, "access_token", "No short-lived access token in Instagram response");
            String appScopedUserId = stringValue(shortLivedTokenPayload, "user_id");
            String grantedScopes = stringValue(shortLivedTokenPayload, "permissions");

            Map<String, Object> longLivedTokenData = exchangeForLongLivedToken(shortLivedAccessToken);
            String finalAccessToken = valueOrDefault(stringValue(longLivedTokenData, "access_token"), shortLivedAccessToken);
            long expiresInSeconds = longValue(longLivedTokenData, "expires_in", 0L);

            Map<String, Object> profileData = fetchProfile(finalAccessToken);
            String instagramId = firstNonBlank(
                    stringValue(profileData, "user_id"),
                    stringValue(profileData, "id"),
                    appScopedUserId
            );
            String username = stringValue(profileData, "username");
            String name = stringValue(profileData, "name");

            if (instagramId == null) {
                throw new RuntimeException("No Instagram user id returned from profile endpoint");
            }

            upsertConnectedAccount(
                    userId,
                    instagramId,
                    appScopedUserId,
                    firstNonBlank(name, username, instagramId),
                    finalAccessToken,
                    firstNonBlank(grantedScopes, REQUESTED_SCOPES),
                    expiresInSeconds
            );

            log.info("Successfully connected Instagram account {} (@{})", instagramId, username);
        } catch (Exception ex) {
            log.error("Failed to handle Instagram OAuth callback", ex);
            throw new RuntimeException("Instagram OAuth callback failed: " + ex.getMessage(), ex);
        }
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

        String resolvedUserId = payloadParts[0];
        long issuedAt = parseEpochSeconds(payloadParts[1]);
        long now = Instant.now().getEpochSecond();
        if (issuedAt <= 0 || now - issuedAt > Math.max(60L, stateTtlSeconds)) {
            throw new RuntimeException("Expired OAuth state");
        }

        return resolvedUserId;
    }

    public String getValidAccessToken(ConnectedAccount account) {
        if (account == null) {
            throw new RuntimeException("Instagram account is required");
        }
        if (account.getAccessTokenEnc() == null || account.getAccessTokenEnc().isBlank()) {
            throw new RuntimeException("Instagram access token not found for account: " + account.getId());
        }

        if (account.getTokenExpiresAt() != null && account.getTokenExpiresAt().isAfter(LocalDateTime.now().plusDays(3))) {
            return account.getAccessTokenEnc();
        }

        return refreshLongLivedToken(account).get("accessToken").toString();
    }

    public Map<String, Object> refreshLongLivedToken(String userId) {
        UUID currentUserId = UUID.fromString(userId);
        ConnectedAccount account = accountRepository.findByUserIdAndPlatform(currentUserId, "INSTAGRAM")
                .orElseThrow(() -> new RuntimeException("Instagram account not connected for user"));
        return refreshLongLivedToken(account);
    }

    public Map<String, Object> refreshLongLivedToken(String userId, UUID accountId) {
        UUID currentUserId = UUID.fromString(userId);
        ConnectedAccount account = accountRepository.findByIdAndUserId(accountId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Instagram account not found"));

        if (!"INSTAGRAM".equalsIgnoreCase(account.getPlatform())) {
            throw new RuntimeException("Account is not an Instagram account");
        }

        return refreshLongLivedToken(account);
    }

    private Map<String, Object> refreshLongLivedToken(ConnectedAccount account) {
        if (account.getAccessTokenEnc() == null || account.getAccessTokenEnc().isBlank()) {
            throw new RuntimeException("Access token missing for Instagram account: " + account.getId());
        }

        String url = refreshTokenUrl
                + "?grant_type=ig_refresh_token"
                + "&access_token=" + urlEncode(account.getAccessTokenEnc());

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
        Map<String, Object> refreshData = response.getBody();
        if (refreshData == null) {
            throw new RuntimeException("Empty response from Instagram token refresh");
        }

        String refreshedAccessToken = requiredValue(refreshData, "access_token", "No access token in Instagram refresh response");
        long expiresIn = longValue(refreshData, "expires_in", 0L);

        account.setAccessTokenEnc(refreshedAccessToken);
        if (expiresIn > 0) {
            account.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
        }
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        Map<String, Object> result = new HashMap<>();
        result.put("accountId", account.getId());
        result.put("platformAccountId", account.getPlatformAccountId());
        result.put("expiresIn", expiresIn);
        result.put("tokenExpiresAt", account.getTokenExpiresAt() != null ? account.getTokenExpiresAt().toString() : null);
        result.put("accessToken", refreshedAccessToken);
        return result;
    }

    private Map<String, Object> exchangeCodeForShortLivedToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(shortLivedTokenUrl, HttpMethod.POST, request, Map.class);
        Map<String, Object> bodyMap = response.getBody();
        if (bodyMap == null) {
            throw new RuntimeException("Empty response from Instagram token exchange");
        }

        return unwrapTokenPayload(bodyMap);
    }

    private Map<String, Object> exchangeForLongLivedToken(String shortLivedAccessToken) {
        String url = longLivedTokenUrl
                + "?grant_type=ig_exchange_token"
                + "&client_secret=" + urlEncode(clientSecret)
                + "&access_token=" + urlEncode(shortLivedAccessToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
        Map<String, Object> bodyMap = response.getBody();
        if (bodyMap == null) {
            throw new RuntimeException("Empty response from Instagram long-lived token exchange");
        }
        return bodyMap;
    }

    private Map<String, Object> fetchProfile(String accessToken) {
        String url = apiBaseUrl
                + "/me?fields=user_id,username,name,biography,profile_picture_url"
                + "&access_token=" + urlEncode(accessToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
        Map<String, Object> profileData = response.getBody();
        if (profileData == null) {
            throw new RuntimeException("No profile information returned from Instagram");
        }
        return profileData;
    }

    private void upsertConnectedAccount(
            String userId,
            String instagramId,
            String appScopedUserId,
            String accountName,
            String accessToken,
            String scopes,
            long expiresInSeconds
    ) {
        UUID currentUserId = UUID.fromString(userId);
        LocalDateTime now = LocalDateTime.now();

        ConnectedAccount account = accountRepository.findByUserIdAndPlatform(currentUserId, "INSTAGRAM")
                .orElseGet(ConnectedAccount::new);

        account.setUserId(currentUserId);
        account.setPlatform("INSTAGRAM");
        account.setAccountType("BUSINESS");
        account.setAccountName(accountName);
        account.setPlatformAccountId(instagramId);
        account.setIgUserId(instagramId);
        account.setInstagramBusinessAccountId(appScopedUserId != null ? appScopedUserId : instagramId);
        account.setAccessTokenEnc(accessToken);
        account.setRefreshTokenEnc(null);
        account.setTokenExpiresAt(expiresInSeconds > 0 ? now.plusSeconds(expiresInSeconds) : null);
        account.setScopes(scopes);
        account.setIsActive(true);
        if (account.getConnectedAt() == null) {
            account.setConnectedAt(now);
        }
        account.setUpdatedAt(now);

        accountRepository.save(account);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrapTokenPayload(Map<String, Object> tokenData) {
        Object dataNode = tokenData.get("data");
        if (dataNode instanceof List<?> dataList && !dataList.isEmpty() && dataList.get(0) instanceof Map<?, ?> firstItem) {
            return (Map<String, Object>) firstItem;
        }
        return tokenData;
    }

    private static String requiredValue(Map<String, Object> data, String key, String errorMessage) {
        String value = stringValue(data, key);
        if (value == null || value.isBlank()) {
            throw new RuntimeException(errorMessage);
        }
        return value;
    }

    private static String stringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private static long longValue(Map<String, Object> data, String key, long defaultValue) {
        try {
            Object value = data.get(key);
            return value != null ? Long.parseLong(String.valueOf(value)) : defaultValue;
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String buildState(String userId) {
        long issuedAt = Instant.now().getEpochSecond();
        String payload = userId + ":" + issuedAt;
        String payloadEncoded = base64UrlEncode(payload.getBytes(StandardCharsets.UTF_8));
        String signatureEncoded = hmacSha256Base64Url(payload, stateSecret);
        return payloadEncoded + "." + signatureEncoded;
    }

    private static String hmacSha256Base64Url(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(digest);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to sign OAuth state", ex);
        }
    }

    private static long parseEpochSeconds(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            return -1L;
        }
    }

    private static String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
