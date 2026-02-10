package com.creatoros.assetservice.service;

import com.creatoros.assetservice.model.AssetFolder;
import com.creatoros.assetservice.repository.AssetFolderRepository;
import com.creatoros.assetservice.repository.MediaFileRepository;
import com.creatoros.assetservice.repository.MediaMetadataRepository;
import com.creatoros.assetservice.repository.MediaProcessingJobRepository;
import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private Storage storage;

    @Mock
    private MediaFileRepository mediaFileRepository;

    @Mock
    private MediaMetadataRepository mediaMetadataRepository;

    @Mock
    private AssetFolderRepository assetFolderRepository;

    @Mock
    private MediaProcessingJobRepository mediaProcessingJobRepository;

    @InjectMocks
    private AssetService assetService;

    private UUID userId;
    private UUID folderId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        folderId = UUID.randomUUID();
    }

    @Test
    void createFolder_ShouldSaveAndReturnFolder() {
        // Arrange
        String folderName = "Test Folder";
        String description = "Test Description";
        
        AssetFolder expectedFolder = AssetFolder.builder()
                .id(folderId)
                .userId(userId)
                .name(folderName)
                .description(description)
                .parentFolderId(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(assetFolderRepository.save(any(AssetFolder.class))).thenReturn(expectedFolder);

        // Act
        AssetFolder createdFolder = assetService.createFolder(folderName, description, userId, null);

        // Assert
        assertNotNull(createdFolder);
        assertEquals(folderName, createdFolder.getName());
        assertEquals(userId, createdFolder.getUserId());
        assertEquals(folderId, createdFolder.getId());
        verify(assetFolderRepository, times(1)).save(any(AssetFolder.class));
    }

    @Test
    void getRootFolders_ShouldReturnListOfFolders() {
        // Arrange
        AssetFolder folder1 = AssetFolder.builder().id(UUID.randomUUID()).userId(userId).name("Folder 1").build();
        when(assetFolderRepository.findByUserIdAndParentFolderIdIsNull(userId))
                .thenReturn(List.of(folder1));

        // Act
        List<AssetFolder> results = assetService.getRootFolders(userId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Folder 1", results.get(0).getName());
        verify(assetFolderRepository, times(1)).findByUserIdAndParentFolderIdIsNull(userId);
    }

    @Test
    void getFolderContents_ShouldReturnFilesAndFolders() {
        // Arrange
        AssetFolder subFolder = AssetFolder.builder().id(UUID.randomUUID()).userId(userId).name("SubFolder").build();
        when(assetFolderRepository.findByUserIdAndParentFolderId(userId, folderId))
                .thenReturn(List.of(subFolder));
        
        when(mediaFileRepository.findByUserIdAndFolderId(userId, folderId))
                .thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> contents = assetService.getFolderContents(userId, folderId);

        // Assert
        assertNotNull(contents);
        assertTrue(contents.containsKey("folders"));
        assertTrue(contents.containsKey("files"));
        
        List<?> folders = (List<?>) contents.get("folders");
        assertEquals(1, folders.size());
        
        verify(assetFolderRepository, times(1)).findByUserIdAndParentFolderId(userId, folderId);
        verify(mediaFileRepository, times(1)).findByUserIdAndFolderId(userId, folderId);
    }
}
