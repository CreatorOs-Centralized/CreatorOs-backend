package com.creatoros.publishing.controllers;

import com.creatoros.publishing.services.YouTubeOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/oauth/youtube")
@RequiredArgsConstructor
@Slf4j
public class YouTubeOAuthController {

    private final YouTubeOAuthService oAuthService;

    /**
     * Get YouTube OAuth authorization URL
     */
    @GetMapping("/login")
    public String login() {
        log.info("Generating YouTube OAuth URL");
        return oAuthService.buildAuthorizationUrl();
    }

    /**
     * Handle OAuth callback from YouTube
     */
    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String code) {
        try {
            log.info("Received YouTube OAuth callback for user {}", userId);
            oAuthService.handleCallback(userId, code);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "YouTube channel connected successfully"
            ));
        } catch (RuntimeException ex) {
            log.error("Error handling YouTube callback: {}", ex.getMessage());
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
                            "error", "Failed to connect YouTube: " + ex.getMessage()
                    ));
        }
    }
}
