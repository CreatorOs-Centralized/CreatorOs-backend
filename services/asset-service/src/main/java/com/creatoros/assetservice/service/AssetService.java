package com.creatoros.assetservice.service;

import com.creatoros.assetservice.model.AssetFolder;
import com.creatoros.assetservice.model.MediaFile;
import com.creatoros.assetservice.model.MediaMetadata;
import com.creatoros.assetservice.model.MediaProcessingJob;
import com.creatoros.assetservice.repository.AssetFolderRepository;
import com.creatoros.assetservice.repository.MediaFileRepository;
import com.creatoros.assetservice.repository.MediaMetadataRepository;
import com.creatoros.assetservice.repository.MediaProcessingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetService {

    private final S3Client s3Client;
    private final MediaFileRepository mediaFileRepository;
    private final MediaMetadataRepository mediaMetadataRepository;
    private final AssetFolderRepository assetFolderRepository;
    private final MediaProcessingJobRepository mediaProcessingJobRepository;

    @Value("${r2.bucket-name}")
    private String bucketName;

    @Value("${r2.endpoint}")
    private String r2Endpoint;

    @Transactional
    public MediaFile uploadFile(MultipartFile file, UUID userId, UUID folderId) throws IOException {
        String fileName = file.getOriginalFilename();
        String storagePath = generateStoragePath(userId, folderId, fileName);

        // Upload to R2
        log.info("Uploading file {} to bucket {} at path {}", fileName, bucketName, storagePath);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(storagePath)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        // Create MediaFile entity
        MediaFile mediaFile = MediaFile.builder()
                .userId(userId)
                .folderId(folderId)
                .fileName(fileName)
                .originalFileName(fileName)
                .fileType(getFileExtension(fileName))
                .mimeType(file.getContentType())
                .sizeBytes(file.getSize())
                .storageProvider("R2")
                .bucketName(bucketName)
                .storagePath(storagePath)
                .publicUrl(constructPublicUrl(storagePath))
                .uploadStatus(MediaFile.UploadStatus.COMPLETED)
                .build();

        mediaFile = mediaFileRepository.save(mediaFile);

        // Create initial Metadata
        MediaMetadata metadata = MediaMetadata.builder()
                .mediaFile(mediaFile)
                .extra(new HashMap<>())
                .build();
        mediaMetadataRepository.save(metadata);

        // Schedule Processing Job (e.g. for thumbnails)
        MediaProcessingJob job = MediaProcessingJob.builder()
                .mediaFile(mediaFile)
                .jobType("METADATA_EXTRACTION")
                .status(MediaProcessingJob.JobStatus.PENDING)
                .attempts(0)
                .build();
        mediaProcessingJobRepository.save(job);

        return mediaFile;
    }

    @Transactional
    public AssetFolder createFolder(String name, String description, UUID userId, UUID parentFolderId) {
        AssetFolder folder = AssetFolder.builder()
                .name(name)
                .description(description)
                .userId(userId)
                .parentFolderId(parentFolderId)
                .build();
        return assetFolderRepository.save(folder);
    }

    public Map<String, Object> getFolderContents(UUID userId, UUID folderId) {
        List<AssetFolder> folders;
        List<MediaFile> files;

        if (folderId == null) {
            folders = assetFolderRepository.findByUserIdAndParentFolderIdIsNull(userId);
            // Assuming root files have a specific handling or just standard validation
            throw new IllegalArgumentException("Root listing not fully defined, please provide folderId"); 
        } else {
            folders = assetFolderRepository.findByUserIdAndParentFolderId(userId, folderId);
            files = mediaFileRepository.findByUserIdAndFolderId(userId, folderId);
        }

        Map<String, Object> contents = new HashMap<>();
        contents.put("folders", folders);
        contents.put("files", files);
        return contents;
    }
    
    public List<AssetFolder> getRootFolders(UUID userId) {
        return assetFolderRepository.findByUserIdAndParentFolderIdIsNull(userId);
    }

    private String generateStoragePath(UUID userId, UUID folderId, String fileName) {
        String safeFileName = UUID.randomUUID() + "-" + fileName.replaceAll("\\s+", "_");
        return String.format("%s/%s/%s", userId, folderId, safeFileName);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private String constructPublicUrl(String storagePath) {
        // This depends on how R2 public access is configured (custom domain vs r2.dev)
        // For now, returning a placeholder or constructed URL based on endpoint
        return r2Endpoint + "/" + bucketName + "/" + storagePath;
    }
}
