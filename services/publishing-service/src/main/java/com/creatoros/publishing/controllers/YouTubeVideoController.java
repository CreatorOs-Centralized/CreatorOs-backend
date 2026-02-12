package com.creatoros.publishing.controllers;

import com.creatoros.publishing.dto.PublishVideoRequest;
import com.creatoros.publishing.entities.ConnectedAccount;
import com.creatoros.publishing.models.PublishResult;
import com.creatoros.publishing.repositories.ConnectedAccountRepository;
import com.creatoros.publishing.services.YouTubeAnalyticsService;
import com.creatoros.publishing.services.YouTubeVideoService;
import com.creatoros.publishing.strategy.YouTubePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/youtube")
@RequiredArgsConstructor
@Slf4j
public class YouTubeVideoController {

    private final YouTubeVideoService youtubeVideoService;
    private final YouTubeAnalyticsService youtubeAnalyticsService;
    private final YouTubePublisher youtubePublisher;
    private final ConnectedAccountRepository accountRepository;

    /**
     * Get all videos from a YouTube channel
     * 
     * @param accountId - Connected account ID
     * @param maxResults - Maximum number of videos to fetch (default: 10, max: 50)
     */
    @GetMapping("/accounts/{accountId}/videos")
    public ResponseEntity<?> getChannelVideos(
            @PathVariable UUID accountId,
            @RequestParam(required = false, defaultValue = "10") Integer maxResults
    ) {
        try {
            if (maxResults > 50) {
                maxResults = 50; // YouTube API limit
            }
            
            log.info("Fetching videos for account: {}, maxResults: {}", accountId, maxResults);
            List<Map<String, Object>> videos = youtubeVideoService.getChannelVideos(accountId, maxResults);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", videos.size(),
                    "videos", videos
            ));
        } catch (RuntimeException ex) {
            log.error("Error fetching videos: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "error", ex.getMessage()
                    ));
        } catch (Exception ex) {
            log.error("Unexpected error: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to fetch videos: " + ex.getMessage()
                    ));
        }
    }

    /**
     * Get specific video details by video ID
     * 
     * @param accountId - Connected account ID
     * @param videoId - YouTube video ID
     */
    @GetMapping("/accounts/{accountId}/videos/{videoId}")
    public ResponseEntity<?> getVideoById(
            @PathVariable UUID accountId,
            @PathVariable String videoId
    ) {
        try {
            log.info("Fetching video {} for account: {}", videoId, accountId);
            Map<String, Object> video = youtubeVideoService.getVideoById(accountId, videoId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "video", video
            ));
        } catch (RuntimeException ex) {
            log.error("Error fetching video: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "error", ex.getMessage()
                    ));
        } catch (Exception ex) {
            log.error("Unexpected error: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to fetch video: " + ex.getMessage()
                    ));
        }
    }

    /**
     * Get channel statistics (subscribers, total views, video count)
     * 
     * @param accountId - Connected account ID
     */
    @GetMapping("/accounts/{accountId}/statistics")
    public ResponseEntity<?> getChannelStatistics(@PathVariable UUID accountId) {
        try {
            log.info("Fetching channel statistics for account: {}", accountId);
            Map<String, Object> stats = youtubeVideoService.getChannelStatistics(accountId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "channel", stats
            ));
        } catch (RuntimeException ex) {
            log.error("Error fetching channel statistics: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "error", ex.getMessage()
                    ));
        } catch (Exception ex) {
            log.error("Unexpected error: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to fetch statistics: " + ex.getMessage()
                    ));
        }
    }

    /**
     * Get video analytics for a specific video
     * 
     * @param accountId - Connected account ID
     * @param videoId - YouTube video ID
     * @param startDate - Start date (YYYY-MM-DD), defaults to 30 days ago
     * @param endDate - End date (YYYY-MM-DD), defaults to today
     */
    @GetMapping("/accounts/{accountId}/videos/{videoId}/analytics")
    public ResponseEntity<?> getVideoAnalytics(
            @PathVariable UUID accountId,
            @PathVariable String videoId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            
            log.info("Fetching analytics for video {} from {} to {}", videoId, start, end);
            Map<String, Object> analytics = youtubeAnalyticsService.getVideoAnalytics(accountId, videoId, start, end);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "analytics", analytics
            ));
        } catch (RuntimeException ex) {
            log.error("Error fetching video analytics: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "error", ex.getMessage()
                    ));
        } catch (Exception ex) {
            log.error("Unexpected error: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to fetch analytics: " + ex.getMessage()
                    ));
        }
    }

    /**
     * Get channel analytics for a date range
     * 
     * @param accountId - Connected account ID
     * @param startDate - Start date (YYYY-MM-DD), defaults to 30 days ago
     * @param endDate - End date (YYYY-MM-DD), defaults to today
     */
    @GetMapping("/accounts/{accountId}/analytics")
    public ResponseEntity<?> getChannelAnalytics(
            @PathVariable UUID accountId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            
            log.info("Fetching channel analytics from {} to {}", start, end);
            Map<String, Object> analytics = youtubeAnalyticsService.getChannelAnalytics(accountId, start, end);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "analytics", analytics
            ));
        } catch (RuntimeException ex) {
            log.error("Error fetching channel analytics: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "error", ex.getMessage()
                    ));
        } catch (Exception ex) {
            log.error("Unexpected error: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to fetch analytics: " + ex.getMessage()
                    ));
        }
    }

    /**
     * Get top performing videos for a date range
     * 
     * @param accountId - Connected account ID
     * @param startDate - Start date (YYYY-MM-DD), defaults to 30 days ago
     * @param endDate - End date (YYYY-MM-DD), defaults to today
     * @param maxResults - Maximum number of videos (default: 10)
     */
    @GetMapping("/accounts/{accountId}/top-videos")
    public ResponseEntity<?> getTopVideos(
            @PathVariable UUID accountId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "10") Integer maxResults
    ) {
        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            
            log.info("Fetching top videos from {} to {}", start, end);
            List<Map<String, Object>> topVideos = youtubeAnalyticsService.getTopVideos(accountId, start, end, maxResults);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", topVideos.size(),
                    "videos", topVideos
            ));
        } catch (RuntimeException ex) {
            log.error("Error fetching top videos: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "error", ex.getMessage()
                    ));
        } catch (Exception ex) {
            log.error("Unexpected error: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to fetch top videos: " + ex.getMessage()
                    ));
        }
    }

    /**
     * Publish a video to YouTube directly
     * 
     * POST /youtube/publish
     * Body: {
     *   "accountId": "uuid",
     *   "title": "Video Title",
     *   "description": "Video Description",
     *   "gcsPath": "videos/my-video.mp4",
     *   "privacyStatus": "public",  // "public", "unlisted", or "private"
     *   "tags": ["tag1", "tag2"],
     *   "categoryId": "22"  // 22 = People & Blogs
     * }
     */
    @PostMapping("/publish")
    public ResponseEntity<?> publishVideo(@RequestBody PublishVideoRequest request) {
        try {
            // Validate request
            if (request.getAccountId() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "accountId is required"
                ));
            }
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "title is required"
                ));
            }
            if (request.getGcsPath() == null || request.getGcsPath().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "gcsPath is required"
                ));
            }

            // Get connected account
            ConnectedAccount account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new RuntimeException("Account not found: " + request.getAccountId()));

            // Validate it's a YouTube account
            if (!"YOUTUBE".equalsIgnoreCase(account.getPlatform())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Account is not a YouTube account"
                ));
            }

            log.info("Publishing video to YouTube: {}", request.getTitle());

            // Call YouTube publisher
            PublishResult result = youtubePublisher.publishDirect(
                    account,
                    request.getTitle(),
                    request.getDescription() != null ? request.getDescription() : "",
                    request.getGcsPath(),
                    request.getPrivacyStatus(),
                    request.getTags(),
                    request.getCategoryId()
            );

            if (result.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "videoId", result.getPlatformPostId(),
                        "permalink", result.getPermalink(),
                        "message", "Video published successfully"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "success", false,
                                "error", result.getErrorMessage()
                        ));
            }

        } catch (RuntimeException ex) {
            log.error("Error publishing video: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "error", ex.getMessage()
                    ));
        } catch (Exception ex) {
            log.error("Unexpected error: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to publish video: " + ex.getMessage()
                    ));
        }
    }
}
