package com.creatoros.publishing.services;

import com.creatoros.publishing.entities.ConnectedAccount;
import com.creatoros.publishing.repositories.ConnectedAccountRepository;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class YouTubeVideoService {

    private final ConnectedAccountRepository accountRepository;
    private final YouTubeTokenService tokenService;

    /**
     * Get all videos from a YouTube channel
     */
    public List<Map<String, Object>> getChannelVideos(UUID userId, UUID accountId, Integer maxResults) {
        
        ConnectedAccount account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

        if (!"YOUTUBE".equals(account.getPlatform())) {
            throw new RuntimeException("Account is not a YouTube account");
        }

        try {
            String accessToken = tokenService.getValidAccessToken(account);
            String channelId = account.getYoutubeChannelId();

            // Build YouTube client
            YouTube youtube = new YouTube.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    request -> request.getHeaders().setAuthorization("Bearer " + accessToken)
            ).setApplicationName("CreatorOS-Publishing").build();

            // Search for videos in the channel
            YouTube.Search.List search = youtube.search().list(List.of("id", "snippet"));
            search.setChannelId(channelId);
            search.setType(List.of("video"));
            search.setOrder("date"); // Most recent first
            search.setMaxResults(maxResults != null ? maxResults.longValue() : 10L);

            SearchListResponse searchResponse = search.execute();
            List<SearchResult> searchResults = searchResponse.getItems();

            if (searchResults.isEmpty()) {
                log.info("No videos found for channel: {}", channelId);
                return new ArrayList<>();
            }

            // Get video IDs
            List<String> videoIds = searchResults.stream()
                    .map(result -> result.getId().getVideoId())
                    .toList();

            // Get video details (statistics)
            YouTube.Videos.List videoList = youtube.videos()
                    .list(List.of("snippet", "contentDetails", "statistics", "status"));
            videoList.setId(videoIds);

            VideoListResponse videoResponse = videoList.execute();
            List<Video> videos = videoResponse.getItems();

            // Build response
            List<Map<String, Object>> result = new ArrayList<>();
            for (Video video : videos) {
                Map<String, Object> videoData = new HashMap<>();
                videoData.put("videoId", video.getId());
                videoData.put("title", video.getSnippet().getTitle());
                videoData.put("description", video.getSnippet().getDescription());
                videoData.put("publishedAt", video.getSnippet().getPublishedAt().toString());
                videoData.put("channelId", video.getSnippet().getChannelId());
                videoData.put("channelTitle", video.getSnippet().getChannelTitle());
                videoData.put("thumbnailUrl", video.getSnippet().getThumbnails().getDefault().getUrl());
                videoData.put("thumbnailHigh", video.getSnippet().getThumbnails().getHigh() != null 
                        ? video.getSnippet().getThumbnails().getHigh().getUrl() : "");
                
                // Statistics
                if (video.getStatistics() != null) {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("viewCount", video.getStatistics().getViewCount());
                    stats.put("likeCount", video.getStatistics().getLikeCount());
                    stats.put("commentCount", video.getStatistics().getCommentCount());
                    videoData.put("statistics", stats);
                }

                // Content details
                if (video.getContentDetails() != null) {
                    videoData.put("duration", video.getContentDetails().getDuration());
                }

                // Status
                if (video.getStatus() != null) {
                    videoData.put("privacyStatus", video.getStatus().getPrivacyStatus());
                }

                videoData.put("url", "https://www.youtube.com/watch?v=" + video.getId());
                
                result.add(videoData);
            }

            log.info("Fetched {} videos for channel: {}", result.size(), channelId);
            return result;

        } catch (Exception ex) {
            log.error("Failed to fetch YouTube videos for account: {}", accountId, ex);
            throw new RuntimeException("Failed to fetch videos: " + ex.getMessage(), ex);
        }
    }

    /**
     * Get specific video details by video ID
     */
    public Map<String, Object> getVideoById(UUID userId, UUID accountId, String videoId) {
        
        ConnectedAccount account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

        try {
            String accessToken = tokenService.getValidAccessToken(account);

            YouTube youtube = new YouTube.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    request -> request.getHeaders().setAuthorization("Bearer " + accessToken)
            ).setApplicationName("CreatorOS-Publishing").build();

            YouTube.Videos.List videoList = youtube.videos()
                    .list(List.of("snippet", "contentDetails", "statistics", "status"));
            videoList.setId(List.of(videoId));

            VideoListResponse response = videoList.execute();
            
            if (response.getItems().isEmpty()) {
                throw new RuntimeException("Video not found: " + videoId);
            }

            Video video = response.getItems().get(0);

            Map<String, Object> videoData = new HashMap<>();
            videoData.put("videoId", video.getId());
            videoData.put("title", video.getSnippet().getTitle());
            videoData.put("description", video.getSnippet().getDescription());
            videoData.put("publishedAt", video.getSnippet().getPublishedAt().toString());
            videoData.put("channelId", video.getSnippet().getChannelId());
            videoData.put("channelTitle", video.getSnippet().getChannelTitle());
            videoData.put("thumbnailUrl", video.getSnippet().getThumbnails().getDefault().getUrl());
            videoData.put("thumbnailHigh", video.getSnippet().getThumbnails().getHigh() != null 
                    ? video.getSnippet().getThumbnails().getHigh().getUrl() : "");
            
            if (video.getStatistics() != null) {
                Map<String, Object> stats = new HashMap<>();
                stats.put("viewCount", video.getStatistics().getViewCount());
                stats.put("likeCount", video.getStatistics().getLikeCount());
                stats.put("commentCount", video.getStatistics().getCommentCount());
                stats.put("favoriteCount", video.getStatistics().getFavoriteCount());
                videoData.put("statistics", stats);
            }

            if (video.getContentDetails() != null) {
                videoData.put("duration", video.getContentDetails().getDuration());
            }

            if (video.getStatus() != null) {
                videoData.put("privacyStatus", video.getStatus().getPrivacyStatus());
            }

            videoData.put("url", "https://www.youtube.com/watch?v=" + video.getId());

            return videoData;

        } catch (Exception ex) {
            log.error("Failed to fetch video {} for account: {}", videoId, accountId, ex);
            throw new RuntimeException("Failed to fetch video: " + ex.getMessage(), ex);
        }
    }

    /**
     * Get channel statistics
     */
    public Map<String, Object> getChannelStatistics(UUID userId, UUID accountId) {
        
        ConnectedAccount account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

        try {
            String accessToken = tokenService.getValidAccessToken(account);
            String channelId = account.getYoutubeChannelId();

            YouTube youtube = new YouTube.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    request -> request.getHeaders().setAuthorization("Bearer " + accessToken)
            ).setApplicationName("CreatorOS-Publishing").build();

            YouTube.Channels.List channelList = youtube.channels()
                    .list(List.of("snippet", "statistics", "contentDetails"));
            channelList.setId(List.of(channelId));

            ChannelListResponse response = channelList.execute();
            
            if (response.getItems().isEmpty()) {
                throw new RuntimeException("Channel not found: " + channelId);
            }

            Channel channel = response.getItems().get(0);

            Map<String, Object> channelData = new HashMap<>();
            channelData.put("channelId", channel.getId());
            channelData.put("title", channel.getSnippet().getTitle());
            channelData.put("description", channel.getSnippet().getDescription());
            channelData.put("customUrl", channel.getSnippet().getCustomUrl());
            channelData.put("publishedAt", channel.getSnippet().getPublishedAt().toString());
            
            if (channel.getStatistics() != null) {
                Map<String, Object> stats = new HashMap<>();
                stats.put("subscriberCount", channel.getStatistics().getSubscriberCount());
                stats.put("videoCount", channel.getStatistics().getVideoCount());
                stats.put("viewCount", channel.getStatistics().getViewCount());
                channelData.put("statistics", stats);
            }

            return channelData;

        } catch (Exception ex) {
            log.error("Failed to fetch channel statistics for account: {}", accountId, ex);
            throw new RuntimeException("Failed to fetch channel statistics: " + ex.getMessage(), ex);
        }
    }
}
