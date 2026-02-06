package com.creatoros.assetservice.controller;

import com.creatoros.assetservice.model.AssetFolder;
import com.creatoros.assetservice.model.MediaFile;
import com.creatoros.assetservice.service.AssetService;
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
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaFile> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") UUID userId,
            @RequestParam("folderId") UUID folderId) throws IOException {
        MediaFile uploadedFile = assetService.uploadFile(file, userId, folderId);
        return ResponseEntity.ok(uploadedFile);
    }

    @PostMapping("/folders")
    public ResponseEntity<AssetFolder> createFolder(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("userId") UUID userId,
            @RequestParam(value = "parentFolderId", required = false) UUID parentFolderId) {
        AssetFolder folder = assetService.createFolder(name, description, userId, parentFolderId);
        return ResponseEntity.ok(folder);
    }

    @GetMapping("/folders/{folderId}")
    public ResponseEntity<Map<String, Object>> getFolderContents(
            @PathVariable UUID folderId,
            @RequestParam("userId") UUID userId) {
        Map<String, Object> contents = assetService.getFolderContents(userId, folderId);
        return ResponseEntity.ok(contents);
    }
    
    @GetMapping("/folders/root")
    public ResponseEntity<List<AssetFolder>> getRootFolders(@RequestParam("userId") UUID userId) {
        return ResponseEntity.ok(assetService.getRootFolders(userId));
    }

    @GetMapping("/view/{fileId}")
    public ResponseEntity<org.springframework.core.io.Resource> viewFile(@PathVariable UUID fileId) throws IOException {
        org.springframework.core.io.Resource fileResource = assetService.downloadFile(fileId);
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
