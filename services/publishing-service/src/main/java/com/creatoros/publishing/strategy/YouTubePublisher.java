package com.creatoros.publishing.strategy;

import com.creatoros.publishing.entities.ConnectedAccount;
import com.creatoros.publishing.models.PublishContext;
import com.creatoros.publishing.models.PublishResult;

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
    private final com.creatoros.publishing.client.AssetServiceClient assetServiceClient;

    @Override
    public PublishResult publish(PublishContext context) {

        ConnectedAccount account = context.getConnectedAccount();
        java.util.UUID mediaId = context.getEvent().getContentItemId();
        java.util.UUID userId = context.getEvent().getUserId();

        log.info("Publishing video to YouTube for account: {}, mediaId: {}", account.getId(), mediaId);

        try {
            // Step 1: Get valid access token
            String accessToken = tokenService.getValidAccessToken(account);

            // Step 2: Build YouTube client
            YouTube youtube = new YouTube.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    request -> request.getHeaders().setAuthorization("Bearer " + accessToken))
                    .setApplicationName("CreatorOS-Publishing").build();

            // Step 3: Get Video Metadata from Asset Service
            log.info("Fetching metadata for mediaId: {}", mediaId);
            com.creatoros.publishing.models.MediaFileDTO mediaMetadata = assetServiceClient.getFileMetadata(mediaId, userId);

            // Step 4: Build YouTube video metadata
            Video video = new Video();

            VideoSnippet snippet = new VideoSnippet();
            snippet.setTitle(getVideoTitle(context));
            snippet.setDescription(getVideoDescription(context));
            snippet.setTags(getTags(context));
            snippet.setCategoryId(getCategoryId(context)); // 22 = People & Blogs
            video.setSnippet(snippet);

            VideoStatus status = new VideoStatus();
            status.setPrivacyStatus(getPrivacyStatus(context)); // Default to public
            video.setStatus(status);

            // Step 5: Get video stream from Asset Service
            log.info("Downloading video stream for mediaId: {}", mediaId);
            try (InputStream videoStream = assetServiceClient.downloadFile(mediaId, userId)) {
                InputStreamContent mediaContent = new InputStreamContent(
                        mediaMetadata.getMimeType(),
                        videoStream);
                mediaContent.setLength(mediaMetadata.getSizeBytes());

                // Step 6: Upload video to YouTube
                log.info("Starting YouTube video upload...");

                YouTube.Videos.Insert videoInsert = youtube.videos().insert(
                        Arrays.asList("snippet", "status"),
                        video,
                        mediaContent);

                // Enable resumable upload
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
                                    log.info("Upload progress: {}%", (int) (uploader.getProgress() * 100));
                                    break;
                                case MEDIA_COMPLETE:
                                    log.info("Upload completed");
                                    break;
                            }
                        });

                Video uploadedVideo = videoInsert.execute();

                String videoId = uploadedVideo.getId();
                String permalink = "https://www.youtube.com/watch?v=" + videoId;

                log.info("Successfully published video to YouTube. Video ID: {}", videoId);

                return PublishResult.builder()
                        .success(true)
                        .platformPostId(videoId)
                        .permalink(permalink)
                        .build();
            }

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
}
