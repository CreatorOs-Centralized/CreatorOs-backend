package com.creatoros.publishing.controllers;

import com.creatoros.publishing.entities.ConnectedAccount;
import com.creatoros.publishing.repositories.ConnectedAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/connected-accounts")
@RequiredArgsConstructor
@Slf4j
public class ConnectedAccountController {

    private final ConnectedAccountRepository accountRepository;

    /**
     * Get all connected accounts for the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllAccounts(
            @RequestHeader("X-User-Id") String userId) {
        List<ConnectedAccount> accounts = accountRepository.findAll();
        
        List<Map<String, Object>> response = accounts.stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get connected accounts by platform for the authenticated user
     */
    @GetMapping("/platform/{platform}")
    public ResponseEntity<List<Map<String, Object>>> getAccountsByPlatform(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String platform) {
        List<ConnectedAccount> accounts = accountRepository.findByPlatform(platform.toUpperCase());
        
        List<Map<String, Object>> response = accounts.stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get connected account by ID
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAccountById(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID accountId) {
        return accountRepository.findById(accountId)
                .map(account -> ResponseEntity.ok(toSummary(account)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Account not found")));
    }

    /**
     * Get YouTube channels for the authenticated user
     */
    @GetMapping("/youtube/channels")
    public ResponseEntity<List<Map<String, Object>>> getYouTubeChannels(
            @RequestHeader("X-User-Id") String userId) {
        List<ConnectedAccount> accounts = accountRepository.findByPlatform("YOUTUBE");
        
        List<Map<String, Object>> response = accounts.stream()
                .map(account -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", account.getId());
                    map.put("channelId", account.getYoutubeChannelId() != null ? account.getYoutubeChannelId() : "");
                    map.put("channelName", account.getAccountName());
                    map.put("platformAccountId", account.getPlatformAccountId());
                    map.put("isActive", account.getIsActive());
                    map.put("connectedAt", account.getConnectedAt() != null ? account.getConnectedAt().toString() : "");
                    map.put("hasRefreshToken", account.getRefreshTokenEnc() != null && !account.getRefreshTokenEnc().isEmpty());
                    map.put("tokenExpiresAt", account.getTokenExpiresAt() != null ? account.getTokenExpiresAt().toString() : "");
                    return map;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Convert account to summary (excludes sensitive tokens)
     */
    private Map<String, Object> toSummary(ConnectedAccount account) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", account.getId());
        map.put("userId", account.getUserId());
        map.put("platform", account.getPlatform());
        map.put("accountType", account.getAccountType());
        map.put("accountName", account.getAccountName());
        map.put("platformAccountId", account.getPlatformAccountId());
        map.put("youtubeChannelId", account.getYoutubeChannelId() != null ? account.getYoutubeChannelId() : "");
        map.put("linkedinAuthorUrn", account.getLinkedinAuthorUrn() != null ? account.getLinkedinAuthorUrn() : "");
        map.put("isActive", account.getIsActive());
        map.put("connectedAt", account.getConnectedAt() != null ? account.getConnectedAt().toString() : "");
        map.put("hasRefreshToken", account.getRefreshTokenEnc() != null && !account.getRefreshTokenEnc().isEmpty());
        map.put("tokenExpiresAt", account.getTokenExpiresAt() != null ? account.getTokenExpiresAt().toString() : "");
        return map;
    }
}
