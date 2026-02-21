package com.creatoros.publishing.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class PublishVideoRequest {
    private UUID contentItemId; // Optional content item ID (for job tracking)
    private UUID mediaId; // Alias for contentItemId/Asset Service ID
    private UUID accountId; // Connected account ID
    private String title; // Video title
    private String description; // Video description
    private String gcsPath; // GCS path to video file (e.g., "videos/xyz.mp4")
    private String privacyStatus; // "public", "unlisted", or "private"
    private List<String> tags; // Video tags
    private String categoryId; // YouTube category ID (default: 22 = People & Blogs)
    private String email; // Optional email for notification payloads
}
