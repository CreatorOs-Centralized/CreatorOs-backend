package com.creatoros.assetservice.controller;

import com.creatoros.assetservice.model.AssetFolder;
import com.creatoros.assetservice.model.MediaFile;
import com.creatoros.assetservice.service.AssetService;
import com.creatoros.assetservice.utils.UserContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@org.springframework.validation.annotation.Validated
public class AssetController {

    private final AssetService assetService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaFile> uploadFile(
            @RequestParam("file") @jakarta.validation.constraints.NotNull(message = "File is required") MultipartFile file,
            @RequestParam("folderId") @jakarta.validation.constraints.NotNull(message = "Folder ID is required") UUID folderId) throws IOException {
        UUID userId = UserContextUtil.getCurrentUserId();
        MediaFile uploadedFile = assetService.uploadFile(file, userId, folderId);
        return ResponseEntity.ok(uploadedFile);
    }

    @PostMapping("/folders")
    public ResponseEntity<AssetFolder> createFolder(
            @RequestParam("name") @jakarta.validation.constraints.NotBlank(message = "Folder name cannot be empty") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "parentFolderId", required = false) UUID parentFolderId) {
        UUID userId = UserContextUtil.getCurrentUserId();
        AssetFolder folder = assetService.createFolder(name, description, userId, parentFolderId);
        return ResponseEntity.ok(folder);
    }

    @GetMapping("/folders/{folderId}")
    public ResponseEntity<Map<String, Object>> getFolderContents(@PathVariable UUID folderId) {
        UUID userId = UserContextUtil.getCurrentUserId();
        Map<String, Object> contents = assetService.getFolderContents(userId, folderId);
        return ResponseEntity.ok(contents);
    }
    
    @GetMapping("/folders/root")
    public ResponseEntity<List<AssetFolder>> getRootFolders() {
        UUID userId = UserContextUtil.getCurrentUserId();
        return ResponseEntity.ok(assetService.getRootFolders(userId));
    }

    @GetMapping("/{fileId}/metadata")
    public ResponseEntity<MediaFile> getFileMetadata(@PathVariable UUID fileId) {
        UUID userId = UserContextUtil.getCurrentUserId();
        return ResponseEntity.ok(assetService.getFileMetadata(fileId, userId));
    }

    @GetMapping("/view/{fileId}")
    public ResponseEntity<org.springframework.core.io.Resource> viewFile(@PathVariable UUID fileId) throws IOException {
        UUID userId = UserContextUtil.getCurrentUserId();
        org.springframework.core.io.Resource fileResource = assetService.downloadFile(fileId, userId);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        
        // Try to determine content type from filename if possible, otherwise default
        try {
             if (fileResource.getFilename() != null && fileResource.getFilename().endsWith(".png")) {
                 mediaType = MediaType.IMAGE_PNG;
             } else if (fileResource.getFilename() != null && fileResource.getFilename().endsWith(".jpg")) {
                 mediaType = MediaType.IMAGE_JPEG;
             }
        } catch (Exception e) {
            // ignore
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(fileResource);
    }
}
