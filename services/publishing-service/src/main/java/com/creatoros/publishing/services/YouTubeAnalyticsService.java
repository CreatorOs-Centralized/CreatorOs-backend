package com.creatoros.publishing.services;

import com.creatoros.publishing.entities.ConnectedAccount;
import com.creatoros.publishing.repositories.ConnectedAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class YouTubeAnalyticsService {

    private final ConnectedAccountRepository accountRepository;
    private final YouTubeTokenService tokenService;
    private final RestTemplate restTemplate;

    /**
     * Get video analytics for a specific date range using REST API
     */
    public Map<String, Object> getVideoAnalytics(
            UUID accountId, 
            String videoId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        ConnectedAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

        try {
            String accessToken = tokenService.getValidAccessToken(account);
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;

            String url = "https://youtubeanalytics.googleapis.com/v2/reports"
                    + "?ids=channel==" + account.getYoutubeChannelId()
                    + "&startDate=" + startDate.format(formatter)
                    + "&endDate=" + endDate.format(formatter)
                    + "&metrics=views,likes,comments,shares,estimatedMinutesWatched,averageViewDuration"
                    + "&dimensions=day"
                    + "&filters=video==" + videoId
                    + "&sort=day";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> data = response.getBody();

            Map<String, Object> result = new HashMap<>();
            result.put("videoId", videoId);
            result.put("startDate", startDate.toString());
            result.put("endDate", endDate.toString());
            result.put("data", data);

            return result;

        } catch (Exception ex) {
            log.error("Failed to fetch video analytics for video: {}", videoId, ex);
            throw new RuntimeException("Failed to fetch video analytics: " + ex.getMessage(), ex);
        }
    }

    /**
     * Get channel analytics for a date range using REST API
     */
    public Map<String, Object> getChannelAnalytics(
            UUID accountId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        ConnectedAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

        try {
            String accessToken = tokenService.getValidAccessToken(account);
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;

            String url = "https://youtubeanalytics.googleapis.com/v2/reports"
                    + "?ids=channel==" + account.getYoutubeChannelId()
                    + "&startDate=" + startDate.format(formatter)
                    + "&endDate=" + endDate.format(formatter)
                    + "&metrics=views,likes,comments,shares,estimatedMinutesWatched,averageViewDuration,subscribersGained,subscribersLost"
                    + "&dimensions=day"
                    + "&sort=day";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> data = response.getBody();

            Map<String, Object> result = new HashMap<>();
            result.put("channelId", account.getYoutubeChannelId());
            result.put("startDate", startDate.toString());
            result.put("endDate", endDate.toString());
            result.put("data", data);

            return result;

        } catch (Exception ex) {
            log.error("Failed to fetch channel analytics for account: {}", accountId, ex);
            throw new RuntimeException("Failed to fetch channel analytics: " + ex.getMessage(), ex);
        }
    }

    /**
     * Get top performing videos for a date range using REST API
     */
    public List<Map<String, Object>> getTopVideos(
            UUID accountId,
            LocalDate startDate,
            LocalDate endDate,
            Integer maxResults
    ) {
        ConnectedAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

        try {
            String accessToken = tokenService.getValidAccessToken(account);
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;

            String url = "https://youtubeanalytics.googleapis.com/v2/reports"
                    + "?ids=channel==" + account.getYoutubeChannelId()
                    + "&startDate=" + startDate.format(formatter)
                    + "&endDate=" + endDate.format(formatter)
                    + "&metrics=views,likes,comments,estimatedMinutesWatched"
                    + "&dimensions=video"
                    + "&sort=-views"
                    + "&maxResults=" + (maxResults != null ? maxResults : 10);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> data = response.getBody();

            List<Map<String, Object>> topVideos = new ArrayList<>();
            
            if (data != null && data.containsKey("rows")) {
                @SuppressWarnings("unchecked")
                List<List<Object>> rows = (List<List<Object>>) data.get("rows");
                
                for (List<Object> row : rows) {
                    Map<String, Object> video = new HashMap<>();
                    video.put("videoId", row.get(0));
                    video.put("views", row.get(1));
                    video.put("likes", row.get(2));
                    video.put("comments", row.get(3));
                    video.put("estimatedMinutesWatched", row.get(4));
                    topVideos.add(video);
                }
            }

            return topVideos;

        } catch (Exception ex) {
            log.error("Failed to fetch top videos for account: {}", accountId, ex);
            throw new RuntimeException("Failed to fetch top videos: " + ex.getMessage(), ex);
        }
    }
}
