package com.creatoros.publishing.models;

import lombok.Data;
import java.util.UUID;

@Data
public class MediaFileDTO {
    private UUID id;
    private String fileName;
    private String fileType;
    private long sizeBytes;
    private String mimeType;
}
