package com.creatoros.publishing.services;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@Slf4j
public class GCSService {

    @Value("${gcp.bucket-name:creatoros-videos}")
    private String bucketName;

    private final Storage storage;

    public GCSService() {
        // Initialize GCS client
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    /**
     * Download video from GCS as InputStream
     * For production: Use streaming for large files
     */
    public InputStream downloadVideo(String gcsPath) {
        try {
            log.info("Downloading video from GCS: gs://{}/{}", bucketName, gcsPath);
            
            Blob blob = storage.get(bucketName, gcsPath);
            
            if (blob == null || !blob.exists()) {
                throw new RuntimeException("Video not found in GCS: " + gcsPath);
            }

            long fileSize = blob.getSize();
            log.info("Video size: {} bytes", fileSize);

            // For small files (< 100MB), load into memory
            if (fileSize < 100 * 1024 * 1024) {
                byte[] content = blob.getContent();
                return new ByteArrayInputStream(content);
            }

            // For larger files, use streaming
            return java.nio.channels.Channels.newInputStream(blob.reader());

        } catch (Exception ex) {
            log.error("Failed to download video from GCS: {}", gcsPath, ex);
            throw new RuntimeException("GCS download failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Get video file size
     */
    public long getVideoSize(String gcsPath) {
        Blob blob = storage.get(bucketName, gcsPath);
        if (blob == null || !blob.exists()) {
            throw new RuntimeException("Video not found in GCS: " + gcsPath);
        }
        return blob.getSize();
    }

    /**
     * Get video content type
     */
    public String getContentType(String gcsPath) {
        Blob blob = storage.get(bucketName, gcsPath);
        if (blob == null || !blob.exists()) {
            return "video/mp4"; // default
        }
        String contentType = blob.getContentType();
        return contentType != null ? contentType : "video/mp4";
    }

    /**
     * Upload video to GCS
     * Returns the GCS path (without bucket name)
     */
    public String uploadVideo(MultipartFile file, UUID userId) throws IOException {
        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".mp4";
            
            String filename = "videos/" + userId + "/" + UUID.randomUUID() + extension;
            
            log.info("Uploading video to GCS: gs://{}/{}", bucketName, filename);
            log.info("File size: {} bytes, content type: {}", file.getSize(), file.getContentType());

            // Create blob info
            BlobId blobId = BlobId.of(bucketName, filename);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType() != null ? file.getContentType() : "video/mp4")
                    .build();

            // Upload file
            Blob blob = storage.create(blobInfo, file.getBytes());
            
            log.info("Video uploaded successfully to: {}", filename);
            
            return filename;

        } catch (Exception ex) {
            log.error("Failed to upload video to GCS", ex);
            throw new IOException("GCS upload failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Delete video from GCS
     */
    public boolean deleteVideo(String gcsPath) {
        try {
            log.info("Deleting video from GCS: gs://{}/{}", bucketName, gcsPath);
            boolean deleted = storage.delete(bucketName, gcsPath);
            
            if (deleted) {
                log.info("Video deleted successfully");
            } else {
                log.warn("Video not found or already deleted: {}", gcsPath);
            }
            
            return deleted;

        } catch (Exception ex) {
            log.error("Failed to delete video from GCS: {}", gcsPath, ex);
            return false;
        }
    }

    /**
     * Check if video exists in GCS
     */
    public boolean videoExists(String gcsPath) {
        Blob blob = storage.get(bucketName, gcsPath);
        return blob != null && blob.exists();
    }
}
