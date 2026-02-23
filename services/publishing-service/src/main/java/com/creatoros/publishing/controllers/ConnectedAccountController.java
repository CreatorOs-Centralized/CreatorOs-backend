package com.creatoros.publishing.controllers;

import com.creatoros.publishing.entities.ConnectedAccount;
import com.creatoros.publishing.repositories.ConnectedAccountRepository;
import com.creatoros.publishing.utils.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class ConnectedAccountController {

    private final ConnectedAccountRepository accountRepository;

    /**
     * Get all connected accounts for the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllAccounts() {
        UUID userId = UserContextUtil.getCurrentUserId();
        List<ConnectedAccount> accounts = accountRepository.findByUserId(userId);
        
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
            @PathVariable String platform) {
        UUID userId = UserContextUtil.getCurrentUserId();
        List<ConnectedAccount> accounts = accountRepository.findAllByUserIdAndPlatformIgnoreCase(userId, platform);
        
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
            @PathVariable UUID accountId) {
        UUID userId = UserContextUtil.getCurrentUserId();
        return accountRepository.findByIdAndUserId(accountId, userId)
                .map(account -> ResponseEntity.ok(toSummary(account)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Account not found")));
    }

    /**
     * Get YouTube channels for the authenticated user
     */
    @GetMapping("/youtube/channels")
    public ResponseEntity<List<Map<String, Object>>> getYouTubeChannels() {
        UUID userId = UserContextUtil.getCurrentUserId();
        List<ConnectedAccount> accounts = accountRepository.findAllByUserIdAndPlatformIgnoreCase(userId, "YOUTUBE");
        
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
