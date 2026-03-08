package com.creatoros.publishing.services;

import com.creatoros.publishing.dto.InstagramPublishRequest;
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstagramContentService {

    private static final String DEFAULT_MEDIA_FIELDS = "id,caption,media_type,media_product_type,media_url,permalink,thumbnail_url,timestamp,username,comments_count,like_count";
    private static final String DEFAULT_ACCOUNT_INSIGHTS_METRICS = 
    "reach, follower_count, website_clicks, profile_views, online_followers, accounts_engaged, total_interactions, likes, comments, shares, saves, replies, engaged_audience_demographics,reached_audience_demographics, follower_demographics, follows_and_unfollows,profile_links_taps, views, threads_likes, threads_replies, reposts, quotes,threads_followers, threads_follower_demographics, content_views, threads_views,threads_clicks, threads_reposts";
    private static final String DEFAULT_MEDIA_INSIGHTS_METRICS = "engagement,impressions,reach";

    private final ConnectedAccountRepository accountRepository;
    private final InstagramOAuthService instagramOAuthService;
    private final RestTemplate restTemplate;

    @Value("${instagram.api-base-url:https://graph.instagram.com/v25.0}")
    private String apiBaseUrl;

    public Map<String, Object> publish(UUID userId, InstagramPublishRequest request) {
        ConnectedAccount account = resolveInstagramAccount(userId, request.getAccountId());
        InstagramApiContext context = resolveApiContext(account);
        String accessToken = context.accessToken();
        String igUserId = context.igUserId();

        Map<String, Object> createResponse = createContainer(igUserId, accessToken, request);
        String containerId = asRequiredString(createResponse, "id", "Instagram did not return a container id");

        Map<String, Object> status = getContainerStatusById(containerId, accessToken);
        String statusCode = stringValue(status.get("status_code"));
        if ("ERROR".equalsIgnoreCase(statusCode) || "EXPIRED".equalsIgnoreCase(statusCode)) {
            throw new RuntimeException("Container is not publishable. status_code=" + statusCode);
        }

        Map<String, Object> publishResponse = publishContainer(igUserId, containerId, accessToken);
        String mediaId = asRequiredString(publishResponse, "id", "Instagram did not return published media id");

        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        Map<String, Object> result = new HashMap<>();
        result.put("accountId", account.getId());
        result.put("igUserId", igUserId);
        result.put("containerId", containerId);
        result.put("containerStatus", statusCode);
        result.put("mediaId", mediaId);
        result.put("mediaPermalink", "https://www.instagram.com/p/" + mediaId + "/");
        result.put("rawCreateResponse", createResponse);
        result.put("rawPublishResponse", publishResponse);
        return result;
    }

    public Map<String, Object> getPosts(UUID userId, UUID accountId, int limit, String after) {
        ConnectedAccount account = resolveInstagramAccount(userId, accountId);
        InstagramApiContext context = resolveApiContext(account);
        String accessToken = context.accessToken();
        String igUserId = context.igUserId();
        int safeLimit = Math.max(1, Math.min(limit, 50));

        StringBuilder url = new StringBuilder(apiBaseUrl)
                .append("/")
                .append(urlEncode(igUserId))
                .append("/media?fields=")
                .append(urlEncode(DEFAULT_MEDIA_FIELDS))
                .append("&limit=")
                .append(safeLimit)
                .append("&access_token=")
                .append(urlEncode(accessToken));

        log.info("Fetching Instagram posts for account {} with limit {} and after {} and igUser {}", accountId, safeLimit, after, igUserId);

        if (after != null && !after.isBlank()) {
            url.append("&after=").append(urlEncode(after));
        }

        log.info("Constructed Instagram API URL: {}", url.toString());

        Map<String, Object> response = get(url.toString());

        log.info("Received response from Instagram API: {}", response);

        Map<String, Object> result = new HashMap<>();
        result.put("accountId", accountId);
        result.put("igUserId", igUserId);
        result.put("posts", response.getOrDefault("data", List.of()));
        result.put("paging", response.getOrDefault("paging", Map.of()));
        return result;
    }

    public Map<String, Object> getPostById(UUID userId, UUID accountId, String mediaId) {
        ConnectedAccount account = resolveInstagramAccount(userId, accountId);
        String accessToken = resolveApiContext(account).accessToken();

        String url = apiBaseUrl
                + "/" + urlEncode(mediaId)
                + "?fields=" + urlEncode(DEFAULT_MEDIA_FIELDS + ",children{media_type,media_url,permalink,id}")
                + "&access_token=" + urlEncode(accessToken);

        return get(url);
    }

    public Map<String, Object> getMediaInsights(UUID userId, UUID accountId, String mediaId, String metrics) {
        ConnectedAccount account = resolveInstagramAccount(userId, accountId);
        String accessToken = resolveApiContext(account).accessToken();
        String metricValue = (metrics == null || metrics.isBlank()) ? DEFAULT_MEDIA_INSIGHTS_METRICS : metrics;

        String url = apiBaseUrl
                + "/" + urlEncode(mediaId)
                + "/insights?metric=" + urlEncode(metricValue)
                + "&access_token=" + urlEncode(accessToken);

        Map<String, Object> response = get(url);
        Map<String, Object> result = new HashMap<>();
        result.put("mediaId", mediaId);
        result.put("metrics", metricValue);
        result.put("data", response.getOrDefault("data", List.of()));
        return result;
    }

    public Map<String, Object> getAccountInsights(
            UUID userId,
            UUID accountId,
            String metrics,
            String period,
            LocalDate since,
            LocalDate until
    ) {
        ConnectedAccount account = resolveInstagramAccount(userId, accountId);
        InstagramApiContext context = resolveApiContext(account);
        String accessToken = context.accessToken();
        String igUserId = context.igUserId();

        String metricValue = (metrics == null || metrics.isBlank()) ? DEFAULT_ACCOUNT_INSIGHTS_METRICS : metrics;
        String periodValue = (period == null || period.isBlank()) ? "day" : period;

        StringBuilder url = new StringBuilder(apiBaseUrl)
                .append("/")
                .append(urlEncode(igUserId))
                .append("/insights?metric=")
                .append(urlEncode(metricValue))
                .append("&period=")
                .append(urlEncode(periodValue));

        if (since != null) {
            url.append("&since=").append(urlEncode(since.format(DateTimeFormatter.ISO_DATE)));
        }
        if (until != null) {
            url.append("&until=").append(urlEncode(until.format(DateTimeFormatter.ISO_DATE)));
        }
        url.append("&access_token=").append(urlEncode(accessToken));

        Map<String, Object> response = get(url.toString());
        Map<String, Object> result = new HashMap<>();
        result.put("accountId", accountId);
        result.put("igUserId", igUserId);
        result.put("metrics", metricValue);
        result.put("period", periodValue);
        result.put("data", response.getOrDefault("data", List.of()));
        return result;
    }

    public Map<String, Object> getPublishingLimit(UUID userId, UUID accountId) {
        ConnectedAccount account = resolveInstagramAccount(userId, accountId);
        InstagramApiContext context = resolveApiContext(account);
        String accessToken = context.accessToken();
        String igUserId = context.igUserId();

        String url = apiBaseUrl
                + "/" + urlEncode(igUserId)
                + "/content_publishing_limit?fields=config,quota_usage"
                + "&access_token=" + urlEncode(accessToken);

        return get(url);
    }

    public Map<String, Object> getContainerStatus(UUID userId, UUID accountId, String containerId) {
        ConnectedAccount account = resolveInstagramAccount(userId, accountId);
        String accessToken = resolveApiContext(account).accessToken();
        return getContainerStatusById(containerId, accessToken);
    }

    private InstagramApiContext resolveApiContext(ConnectedAccount account) {
        String accessToken = instagramOAuthService.getValidAccessToken(account);

        String profileUrl = apiBaseUrl
                + "/me?fields=user_id,id,username"
                + "&access_token=" + urlEncode(accessToken);
        Map<String, Object> profile = get(profileUrl);

        String tokenScopedIgUserId = firstNonBlank(
                stringValue(profile.get("user_id")),
                stringValue(profile.get("id"))
        );

        String storedIgUserId = resolveInstagramUserId(account);
        String effectiveIgUserId = firstNonBlank(tokenScopedIgUserId, storedIgUserId);

        if (effectiveIgUserId == null) {
            throw new RuntimeException("Unable to resolve Instagram user id for this token/account");
        }

        if (tokenScopedIgUserId != null && storedIgUserId != null && !tokenScopedIgUserId.equals(storedIgUserId)) {
            log.warn("Instagram account id mismatch detected. Using token-scoped id {} instead of stored id {} for account {}",
                    tokenScopedIgUserId, storedIgUserId, account.getId());
        }

        return new InstagramApiContext(accessToken, effectiveIgUserId);
    }

    private Map<String, Object> createContainer(String igUserId, String accessToken, InstagramPublishRequest request) {
        String url = apiBaseUrl + "/" + urlEncode(igUserId) + "/media";

        Map<String, Object> payload = new HashMap<>();
        payload.put("access_token", accessToken);

        if (request.getCaption() != null && !request.getCaption().isBlank()) {
            payload.put("caption", request.getCaption());
        }
        if (request.getMediaType() != null && !request.getMediaType().isBlank()) {
            payload.put("media_type", request.getMediaType().trim().toUpperCase());
        }
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            payload.put("image_url", request.getImageUrl().trim());
        }
        if (request.getVideoUrl() != null && !request.getVideoUrl().isBlank()) {
            payload.put("video_url", request.getVideoUrl().trim());
        }
        if (request.getChildren() != null && !request.getChildren().isEmpty()) {
            payload.put("children", String.join(",", request.getChildren()));
        }
        if (Boolean.TRUE.equals(request.getIsCarouselItem())) {
            payload.put("is_carousel_item", true);
        }
        if (request.getThumbOffset() != null && !request.getThumbOffset().isBlank()) {
            payload.put("thumb_offset", request.getThumbOffset());
        }
        if (request.getAltText() != null && !request.getAltText().isBlank()) {
            payload.put("alt_text", request.getAltText());
        }

        validatePublishPayload(payload);
        return post(url, payload);
    }

    private Map<String, Object> publishContainer(String igUserId, String containerId, String accessToken) {
        String url = apiBaseUrl + "/" + urlEncode(igUserId) + "/media_publish";
        Map<String, Object> payload = new HashMap<>();
        payload.put("creation_id", containerId);
        payload.put("access_token", accessToken);
        return post(url, payload);
    }

    private Map<String, Object> getContainerStatusById(String containerId, String accessToken) {
        String url = apiBaseUrl
                + "/" + urlEncode(containerId)
                + "?fields=status_code,status,error_message"
                + "&access_token=" + urlEncode(accessToken);
        return get(url);
    }

    private ConnectedAccount resolveInstagramAccount(UUID userId, UUID accountId) {
        if (accountId == null) {
            throw new RuntimeException("accountId is required");
        }

        ConnectedAccount account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new RuntimeException("Instagram account not found: " + accountId));

        if (!"INSTAGRAM".equalsIgnoreCase(account.getPlatform())) {
            throw new RuntimeException("Account is not an Instagram account");
        }

        if (Boolean.FALSE.equals(account.getIsActive())) {
            throw new RuntimeException("Instagram account is inactive");
        }

        return account;
    }

    private String resolveInstagramUserId(ConnectedAccount account) {
        String igUserId = firstNonBlank(
                account.getIgUserId(),
                account.getPlatformAccountId(),
                account.getInstagramBusinessAccountId()
        );

        if (igUserId == null) {
            throw new RuntimeException("Instagram account is missing IG user id");
        }
        return igUserId;
    }

    private Map<String, Object> get(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(URI.create(url), HttpMethod.GET, entity, Map.class);
            return response.getBody() != null ? response.getBody() : Map.of();
        } catch (HttpStatusCodeException ex) {
            throw new RuntimeException("Instagram API error (GET): " + sanitizeGraphError(ex), ex);
        }
    }

    private Map<String, Object> post(String url, Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            return response.getBody() != null ? response.getBody() : Map.of();
        } catch (HttpStatusCodeException ex) {
            throw new RuntimeException("Instagram API error (POST): " + sanitizeGraphError(ex), ex);
        }
    }

    private static String sanitizeGraphError(HttpStatusCodeException ex) {
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return ex.getStatusCode() + " " + ex.getStatusText();
        }

        if (body.contains("\"code\":2500")) {
            return body + " | Hint: token/account mismatch or app mode role issue (user must be Admin/Developer/Tester in Dev mode).";
        }

        return body;
    }

    private record InstagramApiContext(String accessToken, String igUserId) {
    }

    private static void validatePublishPayload(Map<String, Object> payload) {
        String mediaType = stringValue(payload.get("media_type"));
        String imageUrl = stringValue(payload.get("image_url"));
        String videoUrl = stringValue(payload.get("video_url"));
        String children = stringValue(payload.get("children"));

        if ("CAROUSEL".equalsIgnoreCase(mediaType)) {
            if (children == null || children.isBlank()) {
                throw new RuntimeException("children is required when media_type is CAROUSEL");
            }
            return;
        }

        boolean hasImage = imageUrl != null && !imageUrl.isBlank();
        boolean hasVideo = videoUrl != null && !videoUrl.isBlank();

        if (!hasImage && !hasVideo) {
            throw new RuntimeException("Either imageUrl or videoUrl is required");
        }

        if (hasImage && hasVideo) {
            throw new RuntimeException("Provide only one of imageUrl or videoUrl");
        }

        if (hasVideo && (mediaType == null || mediaType.isBlank())) {
            payload.put("media_type", "REELS");
        }
    }

    private static String asRequiredString(Map<String, Object> data, String key, String errorMessage) {
        String value = stringValue(data.get(key));
        if (value == null || value.isBlank()) {
            throw new RuntimeException(errorMessage);
        }
        return value;
    }

    private static String stringValue(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}