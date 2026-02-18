package com.creatoros.publishing.strategy;

import com.creatoros.publishing.entities.ConnectedAccount;
import com.creatoros.publishing.models.PublishContext;
import com.creatoros.publishing.models.PublishResult;
import com.creatoros.publishing.services.GCSService;
import com.creatoros.publishing.services.YouTubeTokenService;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Component("YOUTUBE")
@RequiredArgsConstructor
@Slf4j
public class YouTubePublisher implements SocialPublisher {

    private final YouTubeTokenService tokenService;
    private final GCSService gcsService;

    @Override
    public PublishResult publish(PublishContext context) {
        
        ConnectedAccount account = context.getConnectedAccount();
        
        log.info("Publishing video to YouTube for account: {}", account.getId());

        try {
            // Step 1: Get valid access token (auto-refresh if needed)
            String accessToken = tokenService.getValidAccessToken(account);

            // Step 2: Build YouTube client
            YouTube youtube = new YouTube.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    request -> request.getHeaders().setAuthorization("Bearer " + accessToken)
            ).setApplicationName("CreatorOS-Publishing").build();

            // Step 3: Build video metadata
            Video videoMetadata = new Video();
            
            // Snippet (title, description, tags)
            VideoSnippet snippet = new VideoSnippet();
            snippet.setTitle(getVideoTitle(context));
            snippet.setDescription(getVideoDescription(context));
            snippet.setTags(getTags(context));
            snippet.setCategoryId(getCategoryId(context)); // 22 = People & Blogs
            videoMetadata.setSnippet(snippet);

            // Status (privacy)
            VideoStatus status = new VideoStatus();
            status.setPrivacyStatus(getPrivacyStatus(context)); // "public", "unlisted", or "private"
            status.setSelfDeclaredMadeForKids(false);
            videoMetadata.setStatus(status);

            // Step 4: Get video file from GCS
            // TODO: In real implementation, get GCS path from content service
            String gcsPath = getGcsPath(context);
            
            log.info("Fetching video from GCS: {}", gcsPath);
            InputStream videoStream = gcsService.downloadVideo(gcsPath);
            String contentType = gcsService.getContentType(gcsPath);
            
            InputStreamContent mediaContent = new InputStreamContent(
                    contentType,
                    videoStream
            );

            // Step 5: Upload video to YouTube
            log.info("Starting YouTube video upload...");
            
            YouTube.Videos.Insert videoInsert = youtube.videos().insert(
                    Arrays.asList("snippet", "status"),
                    videoMetadata,
                    mediaContent
            );

            // Enable resumable upload for large files
            videoInsert.getMediaHttpUploader()
                    .setDirectUploadEnabled(false)
                    .setProgressListener(uploader -> {
                        switch (uploader.getUploadState()) {
                            case INITIATION_STARTED:
                                log.info("Upload initiation started");
                                break;
                            case INITIATION_COMPLETE:
                                log.info("Upload initiation completed");
                                break;
                            case MEDIA_IN_PROGRESS:
                                log.info("Upload progress: {}%", 
                                        (int) (uploader.getProgress() * 100));
                                break;
                            case MEDIA_COMPLETE:
                                log.info("Upload completed");
                                break;
                        }
                    });

            // Execute the upload
            Video uploadedVideo = videoInsert.execute();
            
            String videoId = uploadedVideo.getId();
            String permalink = "https://www.youtube.com/watch?v=" + videoId;

            log.info("Successfully published video to YouTube. Video ID: {}", videoId);

            return PublishResult.builder()
                    .success(true)
                    .platformPostId(videoId)
                    .permalink(permalink)
                    .build();

        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException ex) {
            // Handle YouTube API specific errors
            log.error("YouTube API error: {} - {}", ex.getStatusCode(), ex.getDetails().getMessage());
            
            String errorMessage = String.format("YouTube API error [%d]: %s", 
                    ex.getStatusCode(), 
                    ex.getDetails().getMessage());
            
            // Check for quota exceeded
            if (ex.getStatusCode() == 403 && 
                    ex.getDetails().getMessage().contains("quotaExceeded")) {
                errorMessage = "YouTube API quota exceeded. Please try again tomorrow.";
            }
            
            return PublishResult.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();

        } catch (Exception ex) {
            log.error("Failed to publish video to YouTube", ex);
            return PublishResult.builder()
                    .success(false)
                    .errorMessage("Upload failed: " + ex.getMessage())
                    .build();
        }
    }

    /**
     * Get video title from context
     * TODO: Fetch from content service
     */
    private String getVideoTitle(PublishContext context) {
        // In production, fetch from content service via contentItemId
        if (context.getEvent().getTitle() != null && !context.getEvent().getTitle().isBlank()) {
            return context.getEvent().getTitle();
        }
        return "Video Title - " + context.getEvent().getContentItemId();
    }

    /**
     * Get video description from context
     * TODO: Fetch from content service
     */
    private String getVideoDescription(PublishContext context) {
        // In production, fetch from content service via contentItemId
        if (context.getEvent().getDescription() != null && !context.getEvent().getDescription().isBlank()) {
            return context.getEvent().getDescription();
        }
        return "Video description goes here.\n\nPublished via CreatorOS";
    }

    /**
     * Get privacy status
     * TODO: Make configurable per publish request
     */
    private String getPrivacyStatus(PublishContext context) {
        // Options: "public", "unlisted", "private"
        if (context.getEvent().getPrivacyStatus() != null && !context.getEvent().getPrivacyStatus().isBlank()) {
            return context.getEvent().getPrivacyStatus();
        }
        return "public";
    }

    private List<String> getTags(PublishContext context) {
        if (context.getEvent().getTags() != null && !context.getEvent().getTags().isEmpty()) {
            return context.getEvent().getTags();
        }
        return Arrays.asList("CreatorOS", "Automated Upload");
    }

    private String getCategoryId(PublishContext context) {
        if (context.getEvent().getCategoryId() != null && !context.getEvent().getCategoryId().isBlank()) {
            return context.getEvent().getCategoryId();
        }
        return "22";
    }

    private String getGcsPath(PublishContext context) {
        if (context.getEvent().getGcsPath() != null && !context.getEvent().getGcsPath().isBlank()) {
            return context.getEvent().getGcsPath();
        }
        return "videos/" + context.getEvent().getContentItemId() + ".mp4";
    }

    /**
     * Direct publish method for REST API
     */
    public PublishResult publishDirect(
            ConnectedAccount account,
            String title,
            String description,
            String gcsPath,
            String privacyStatus,
            List<String> tags,
            String categoryId
    ) {
        log.info("Direct publishing video to YouTube for account: {}", account.getId());

        try {
            // Step 1: Get valid access token (auto-refresh if needed)
            String accessToken = tokenService.getValidAccessToken(account);

            // Step 2: Build YouTube client
            YouTube youtube = new YouTube.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    request -> request.getHeaders().setAuthorization("Bearer " + accessToken)
            ).setApplicationName("CreatorOS-Publishing").build();

            // Step 3: Build video metadata
            Video videoMetadata = new Video();
            
            // Snippet (title, description, tags)
            VideoSnippet snippet = new VideoSnippet();
            snippet.setTitle(title);
            snippet.setDescription(description);
            snippet.setTags(tags != null && !tags.isEmpty() ? tags : Arrays.asList("CreatorOS"));
            snippet.setCategoryId(categoryId != null ? categoryId : "22"); // 22 = People & Blogs
            videoMetadata.setSnippet(snippet);

            // Status (privacy)
            VideoStatus status = new VideoStatus();
            status.setPrivacyStatus(privacyStatus != null ? privacyStatus : "public");
            status.setSelfDeclaredMadeForKids(false);
            videoMetadata.setStatus(status);

            // Step 4: Get video file from GCS
            log.info("Fetching video from GCS: {}", gcsPath);
            InputStream videoStream = gcsService.downloadVideo(gcsPath);
            String contentType = gcsService.getContentType(gcsPath);
            
            InputStreamContent mediaContent = new InputStreamContent(
                    contentType,
                    videoStream
            );

            // Step 5: Upload video to YouTube
            log.info("Starting YouTube video upload...");
            
            YouTube.Videos.Insert videoInsert = youtube.videos().insert(
                    Arrays.asList("snippet", "status"),
                    videoMetadata,
                    mediaContent
            );

            // Enable resumable upload for large files
            videoInsert.getMediaHttpUploader()
                    .setDirectUploadEnabled(false)
                    .setProgressListener(uploader -> {
                        switch (uploader.getUploadState()) {
                            case INITIATION_STARTED:
                                log.info("Upload initiation started");
                                break;
                            case INITIATION_COMPLETE:
                                log.info("Upload initiation completed");
                                break;
                            case MEDIA_IN_PROGRESS:
                                log.info("Upload progress: {}%", 
                                        (int) (uploader.getProgress() * 100));
                                break;
                            case MEDIA_COMPLETE:
                                log.info("Upload completed");
                                break;
                        }
                    });

            // Execute the upload
            Video uploadedVideo = videoInsert.execute();
            
            String videoId = uploadedVideo.getId();
            String permalink = "https://www.youtube.com/watch?v=" + videoId;

            log.info("Successfully published video to YouTube. Video ID: {}", videoId);

            return PublishResult.builder()
                    .success(true)
                    .platformPostId(videoId)
                    .permalink(permalink)
                    .build();

        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException ex) {
            // Handle YouTube API specific errors
            log.error("YouTube API error: {} - {}", ex.getStatusCode(), ex.getDetails().getMessage());
            
            String errorMessage = String.format("YouTube API error [%d]: %s", 
                    ex.getStatusCode(), 
                    ex.getDetails().getMessage());
            
            // Check for quota exceeded
            if (ex.getStatusCode() == 403 && 
                    ex.getDetails().getMessage().contains("quotaExceeded")) {
                errorMessage = "YouTube API quota exceeded. Please try again tomorrow.";
            }
            
            return PublishResult.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();

        } catch (Exception ex) {
            log.error("Failed to publish video to YouTube", ex);
            return PublishResult.builder()
                    .success(false)
                    .errorMessage("Upload failed: " + ex.getMessage())
                    .build();
        }
    }
}
