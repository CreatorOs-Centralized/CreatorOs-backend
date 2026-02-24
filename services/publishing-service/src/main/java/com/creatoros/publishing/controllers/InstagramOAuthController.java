package com.creatoros.publishing.controllers;

import com.creatoros.publishing.services.InstagramOAuthService;
import com.creatoros.publishing.utils.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/instagram")
@RequiredArgsConstructor
@Slf4j
public class InstagramOAuthController {

    private final InstagramOAuthService oAuthService;

    /**
     * Get Instagram OAuth authorization URL
     */
    @GetMapping("/login")
    public String login() {
        String userId = UserContextUtil.getCurrentUserId().toString();
        log.info("Generating Instagram OAuth URL for user: {}", userId);
        return oAuthService.buildAuthorizationUrl(userId);
    }

    /**
     * Handle OAuth callback from Instagram
     */
    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam String code) {
        try {
            String userId = UserContextUtil.getCurrentUserId().toString();
            log.info("Received Instagram OAuth callback for user: {}", userId);
            oAuthService.handleCallback(userId, code);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Instagram business account connected successfully"
            ));
        } catch (RuntimeException ex) {
            log.error("Error handling Instagram callback: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "error", ex.getMessage()
                    ));
        } catch (Exception ex) {
            log.error("Unexpected error: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to connect Instagram: " + ex.getMessage()
                    ));
        }
    }
}
