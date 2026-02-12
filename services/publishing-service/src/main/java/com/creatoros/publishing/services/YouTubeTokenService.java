package com.creatoros.publishing.services;

import com.creatoros.publishing.entities.ConnectedAccount;
import com.creatoros.publishing.repositories.ConnectedAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class YouTubeTokenService {

    private final ConnectedAccountRepository repository;
    private final RestTemplate restTemplate;

    @Value("${youtube.client-id}")
    private String clientId;

    @Value("${youtube.client-secret}")
    private String clientSecret;

    @Value("${youtube.token-url}")
    private String tokenUrl;

    /**
     * Get a valid access token, refreshing if necessary
     */
    public String getValidAccessToken(ConnectedAccount account) {
        
        // Check if token is still valid (with 5-minute buffer)
        if (account.getTokenExpiresAt() != null &&
                account.getTokenExpiresAt().isAfter(LocalDateTime.now().plusMinutes(5))) {
            log.debug("Using existing access token for account: {}", account.getId());
            return account.getAccessTokenEnc();
        }

        log.info("Access token expired or missing, refreshing for account: {}", account.getId());
        
        // Refresh the token
        return refreshAccessToken(account);
    }

    /**
     * Refresh the access token using the refresh token
     */
    private String refreshAccessToken(ConnectedAccount account) {
        
        if (account.getRefreshTokenEnc() == null || account.getRefreshTokenEnc().isEmpty()) {
            throw new RuntimeException("Refresh token not found for account: " + account.getId());
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("refresh_token", account.getRefreshTokenEnc());
            body.add("grant_type", "refresh_token");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            Map<String, Object> response = restTemplate.postForObject(
                    tokenUrl,
                    request,
                    Map.class
            );

            if (response == null) {
                throw new RuntimeException("Empty response from token refresh");
            }

            String newAccessToken = (String) response.get("access_token");
            Integer expiresIn = (Integer) response.get("expires_in");

            if (newAccessToken == null) {
                throw new RuntimeException("No access token in refresh response");
            }

            // Update account with new token and expiry
            account.setAccessTokenEnc(newAccessToken);
            account.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn != null ? expiresIn : 3600));
            account.setUpdatedAt(LocalDateTime.now());
            repository.save(account);

            log.info("Successfully refreshed access token for account: {}", account.getId());
            return newAccessToken;

        } catch (Exception ex) {
            log.error("Failed to refresh token for account: {}", account.getId(), ex);
            throw new RuntimeException("Token refresh failed: " + ex.getMessage(), ex);
        }
    }
}
