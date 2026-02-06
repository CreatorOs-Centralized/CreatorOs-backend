package com.creatoros.assetservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "media_files")
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID folderId;

    @Column(nullable = false)
    private String fileName;

    private String originalFileName;

    private String fileType;

    private String mimeType;

    private Long sizeBytes;

    @Column(nullable = false)
    private String storageProvider; // e.g., R2

    private String bucketName;

    private String storagePath;

    private String publicUrl;

    private String checksum;

    @Enumerated(EnumType.STRING)
    private UploadStatus uploadStatus;

    @Builder.Default
    private Boolean isDeleted = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum UploadStatus {
        PENDING,
        UPLOADING,
        COMPLETED,
        FAILED
    }
}
