package com.creatoros.publishing.client;

import com.creatoros.publishing.models.MediaFileDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssetServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.asset-service.url:http://asset-service:8084}")
    private String assetServiceUrl;

    public MediaFileDTO getFileMetadata(UUID fileId) {
        String url = String.format("%s/%s/metadata", assetServiceUrl, fileId);
        log.debug("Fetching metadata from: {}", url);
        return restTemplate.getForObject(url, MediaFileDTO.class);
    }

    public InputStream downloadFile(UUID fileId) {
        String url = String.format("%s/view/%s", assetServiceUrl, fileId);
        log.debug("Downloading file stream from: {}", url);

        // Execute request and return the input stream directly from response
        return restTemplate.execute(url, org.springframework.http.HttpMethod.GET, null, clientHttpResponse -> {
            return clientHttpResponse.getBody();
        });
    }
}
