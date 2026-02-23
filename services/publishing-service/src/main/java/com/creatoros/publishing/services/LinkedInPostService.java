package com.creatoros.publishing.services;

import com.creatoros.publishing.entities.ConnectedAccount;
import com.creatoros.publishing.repositories.ConnectedAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkedInPostService {

    private final ConnectedAccountRepository accountRepository;
    private final RestTemplate restTemplate;

    /**
     * Get user's LinkedIn posts
     */
    public Map<String, Object> getUserPosts(UUID userId, UUID accountId) {
        try {
            Optional<ConnectedAccount> account = accountRepository.findByIdAndUserId(accountId, userId);
            
            if (account.isEmpty()) {
                throw new RuntimeException("Connected account not found for ID: " + accountId);
            }

            ConnectedAccount linkedinAccount = account.get();
            String accessToken = linkedinAccount.getAccessTokenEnc();
            String personUrn = linkedinAccount.getLinkedinAuthorUrn();

            log.info("Fetching posts for personUrn: {}", personUrn);

            if (accessToken == null || accessToken.isEmpty()) {
                throw new RuntimeException("Access token is empty for account: " + accountId);
            }

            if (personUrn == null || personUrn.isEmpty()) {
                throw new RuntimeException("LinkedIn Author URN is empty for account: " + accountId);
            }

            // Build headers with proper LinkedIn versioning
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Accept", "application/json");
            headers.set("LinkedIn-Version", "202401");
            headers.set("X-Restli-Protocol-Version", "2.0.0");
            headers.setAccept(MediaType.parseMediaTypes("application/json"));

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Properly encode the personUrn for URL
            String encodedUrn = URLEncoder.encode(personUrn, "UTF-8");
            String url = "https://api.linkedin.com/v2/ugcPosts?q=authors&authors=" + encodedUrn + "&count=10";

            log.debug("LinkedIn API URL: {}", url);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            log.info("Successfully fetched posts for account: {}", accountId);
            return response.getBody();
        } catch (Exception ex) {
            log.error("Error fetching LinkedIn posts for account: {}", accountId, ex);
            throw new RuntimeException("Failed to fetch LinkedIn posts: " + ex.getMessage(), ex);
        }
    }

    /**
     * Publish a post to LinkedIn
     */
    public Map<String, Object> publishPost(UUID userId, UUID accountId, String postText) {
        try {
            Optional<ConnectedAccount> account = accountRepository.findByIdAndUserId(accountId, userId);
            
            if (account.isEmpty()) {
                throw new RuntimeException("Connected account not found for ID: " + accountId);
            }

            ConnectedAccount linkedinAccount = account.get();
            String accessToken = linkedinAccount.getAccessTokenEnc();
            String personUrn = linkedinAccount.getLinkedinAuthorUrn();

            if (accessToken == null || accessToken.isEmpty()) {
                throw new RuntimeException("Access token is empty for account: " + accountId);
            }

            if (personUrn == null || personUrn.isEmpty()) {
                throw new RuntimeException("LinkedIn Author URN is empty for account: " + accountId);
            }

            log.info("Publishing post for personUrn: {}", personUrn);

            // Build request payload
            Map<String, Object> payload = Map.of(
                    "author", personUrn,
                    "lifecycleState", "PUBLISHED",
                    "specificContent", Map.of(
                            "com.linkedin.ugc.UGCPost", Map.of(
                                    "shareCommentary", Map.of(
                                            "text", postText
                                    ),
                                    "shareMediaCategory", "NONE"
                            )
                    ),
                    "visibility", Map.of(
                            "com.linkedin.ugc.UGCPost", "PUBLIC"
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("LinkedIn-Version", "202401");
            headers.set("X-Restli-Protocol-Version", "2.0.0");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.linkedin.com/v2/ugcPosts",
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            log.info("Successfully published post for account: {}", accountId);
            return response.getBody();
        } catch (Exception ex) {
            log.error("Error publishing post for account: {}", accountId, ex);
            throw new RuntimeException("Failed to publish LinkedIn post: " + ex.getMessage(), ex);
        }
    }
}
