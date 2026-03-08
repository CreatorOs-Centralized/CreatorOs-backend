package com.creatoros.publishing.services;

import com.creatoros.publishing.entities.ConnectedAccount;
import com.creatoros.publishing.repositories.ConnectedAccountRepository;
import com.creatoros.publishing.repositories.PublishedPostRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetaDataDeletionService {

    private final ConnectedAccountRepository connectedAccountRepository;
    private final PublishedPostRepository publishedPostRepository;
    private final ObjectMapper objectMapper;

    @Value("${meta.app-secret:${instagram.client-secret:}}")
    private String metaAppSecret;

    @Value("${meta.data-deletion-status-base-url:https://creatoros-api.adharbattulwar.com}")
    private String statusBaseUrl;

    private final Map<String, Map<String, Object>> deletionRequests = new ConcurrentHashMap<>();

    @Transactional
    public Map<String, Object> processDeletionCallback(String signedRequest) {
        Map<String, Object> payload = parseAndVerifySignedRequest(signedRequest);
        String metaUserId = stringValue(payload.get("user_id"));

        if (metaUserId == null || metaUserId.isBlank()) {
            throw new RuntimeException("signed_request payload missing user_id");
        }

        List<ConnectedAccount> matchedAccounts = connectedAccountRepository
                .findByPlatformAccountIdOrIgUserIdOrInstagramBusinessAccountId(metaUserId, metaUserId, metaUserId);

        long deletedPosts = 0L;
        for (ConnectedAccount account : matchedAccounts) {
            deletedPosts += publishedPostRepository.deleteByConnectedAccountId(account.getId());
        }

        int deletedAccounts = matchedAccounts.size();
        if (!matchedAccounts.isEmpty()) {
            connectedAccountRepository.deleteAll(matchedAccounts);
        }

        String confirmationCode = UUID.randomUUID().toString();
        Map<String, Object> status = new HashMap<>();
        status.put("confirmationCode", confirmationCode);
        status.put("metaUserId", metaUserId);
        status.put("status", "completed");
        status.put("deletedConnectedAccounts", deletedAccounts);
        status.put("deletedPublishedPosts", deletedPosts);
        status.put("completedAtEpoch", Instant.now().getEpochSecond());
        deletionRequests.put(confirmationCode, status);

        String statusUrl = statusBaseUrl.replaceAll("/+$", "") + "/oauth/meta/data-deletion/status/" + confirmationCode;

        Map<String, Object> response = new HashMap<>();
        response.put("url", statusUrl);
        response.put("confirmation_code", confirmationCode);
        return response;
    }

    public Map<String, Object> getDeletionStatus(String confirmationCode) {
        Map<String, Object> status = deletionRequests.get(confirmationCode);
        if (status == null) {
            throw new RuntimeException("Deletion request not found for confirmation code");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("confirmation_code", confirmationCode);
        response.put("status", status.get("status"));
        response.put("meta_user_id", status.get("metaUserId"));
        response.put("deleted_connected_accounts", status.get("deletedConnectedAccounts"));
        response.put("deleted_published_posts", status.get("deletedPublishedPosts"));
        response.put("completed_at_epoch", status.get("completedAtEpoch"));
        return response;
    }

    private Map<String, Object> parseAndVerifySignedRequest(String signedRequest) {
        if (signedRequest == null || signedRequest.isBlank()) {
            throw new RuntimeException("Missing signed_request");
        }

        if (metaAppSecret == null || metaAppSecret.isBlank()) {
            throw new RuntimeException("Missing meta.app-secret configuration");
        }

        String[] parts = signedRequest.split("\\.", 2);
        if (parts.length != 2) {
            throw new RuntimeException("Invalid signed_request format");
        }

        byte[] providedSignature = base64UrlDecode(parts[0]);
        String encodedPayload = parts[1];
        byte[] expectedSignature = hmacSha256(encodedPayload, metaAppSecret);

        if (!java.security.MessageDigest.isEqual(providedSignature, expectedSignature)) {
            throw new RuntimeException("Invalid signed_request signature");
        }

        String payloadJson = new String(base64UrlDecode(encodedPayload), StandardCharsets.UTF_8);
        try {
            Map<String, Object> payload = objectMapper.readValue(payloadJson, new TypeReference<>() {
            });
            String algorithm = stringValue(payload.get("algorithm"));
            if (algorithm != null && !"HMAC-SHA256".equalsIgnoreCase(algorithm)) {
                throw new RuntimeException("Unsupported signed_request algorithm: " + algorithm);
            }
            return payload;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse signed_request payload", ex);
        }
    }

    private static byte[] hmacSha256(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to validate signed_request", ex);
        }
    }

    private static byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private static String stringValue(Object value) {
        return value != null ? String.valueOf(value) : null;
    }
}