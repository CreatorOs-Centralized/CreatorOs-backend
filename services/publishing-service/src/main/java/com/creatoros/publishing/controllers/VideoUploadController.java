package com.creatoros.publishing.controllers;

import com.creatoros.publishing.services.GCSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
@Slf4j
public class VideoUploadController {

    private final GCSService gcsService;

    /**
     * Upload a video to Google Cloud Storage
     * 
     * POST /videos/upload
     * Form data:
     * - file: video file (multipart)
     * - userId: UUID of the user uploading (optional, generates random if not provided)
     * 
     * Returns the GCS path that can be used for publishing
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) String userId
    ) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "File is empty"
                ));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("video/")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "File must be a video. Content-Type: " + contentType
                ));
            }

            // Parse or generate userId
            UUID userIdUuid;
            if (userId != null && !userId.trim().isEmpty()) {
                try {
                    userIdUuid = UUID.fromString(userId);
                } catch (IllegalArgumentException ex) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "error", "Invalid userId format. Must be a valid UUID."
                    ));
                }
            } else {
                userIdUuid = UUID.randomUUID(); // Generate random user ID
            }

            log.info("Uploading video: {} (size: {} bytes, type: {})", 
                    file.getOriginalFilename(), 
                    file.getSize(), 
                    contentType);

            // Upload to GCS
            String gcsPath = gcsService.uploadVideo(file, userIdUuid);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "gcsPath", gcsPath,
                    "fileName", file.getOriginalFilename(),
                    "size", file.getSize(),
                    "contentType", contentType,
                    "message", "Video uploaded successfully to Google Cloud Storage"
            ));

        } catch (Exception ex) {
            log.error("Error uploading video", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to upload video: " + ex.getMessage()
                    ));
        }
    }

    /**
     * Delete a video from Google Cloud Storage
     * 
     * DELETE /videos/{gcsPath}
     * Path variable should be URL encoded
     */
    @DeleteMapping("/{gcsPath}")
    public ResponseEntity<?> deleteVideo(@PathVariable String gcsPath) {
        try {
            log.info("Deleting video: {}", gcsPath);

            boolean deleted = gcsService.deleteVideo(gcsPath);

            if (deleted) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Video deleted successfully"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "success", false,
                                "error", "Video not found"
                        ));
            }

        } catch (Exception ex) {
            log.error("Error deleting video", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to delete video: " + ex.getMessage()
                    ));
        }
    }
}
