package com.creatoros.publishing.client;

import com.creatoros.publishing.models.MediaFileDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import java.io.InputStream;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssetServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.asset-service.url:http://asset-service:8084}")
    private String assetServiceUrl;

    @Value("${creatoros.security.jwt.secret}")
    private String jwtSecret;

    public MediaFileDTO getFileMetadata(UUID fileId, UUID userId) {
        String url = String.format("%s/%s/metadata", assetServiceUrl, fileId);
        log.debug("Fetching metadata from: {}", url);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(buildServiceToken(userId));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<MediaFileDTO> response = restTemplate.exchange(url, HttpMethod.GET, entity, MediaFileDTO.class);
        return response.getBody();
    }

    public InputStream downloadFile(UUID fileId, UUID userId) {
        String url = String.format("%s/view/%s", assetServiceUrl, fileId);
        log.debug("Downloading file stream from: {}", url);

        // Execute request and return the input stream directly from response
        return restTemplate.execute(url, org.springframework.http.HttpMethod.GET, request -> {
            request.getHeaders().setBearerAuth(buildServiceToken(userId));
        }, clientHttpResponse -> {
            return clientHttpResponse.getBody();
        });
    }

    private String buildServiceToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(300);
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("roles", List.of("USER"))
                .signWith(key)
                .compact();
    }
}
