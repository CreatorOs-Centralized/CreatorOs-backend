package com.creatoros.publishing.controllers;

import com.creatoros.publishing.services.FacebookOAuthService;
import com.creatoros.publishing.utils.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class FacebookOAuthController {

    private final FacebookOAuthService oAuthService;

    /**
     * Get Facebook OAuth authorization URL
     */
    @GetMapping("/oauth/facebook/login")
    public String login() {
        String userId = UserContextUtil.getCurrentUserId().toString();
        log.info("Generating Facebook OAuth URL for user: {}", userId);
        return oAuthService.buildAuthorizationUrl(userId);
    }

    /**
     * Handle OAuth callback from Facebook
     */
    @GetMapping("/oauth/facebook/callback")
    public ResponseEntity<?> callback(
            @RequestParam String code,
            @RequestParam String state) {
        try {
            String userId = oAuthService.resolveState(state);
            log.info("Received Facebook OAuth callback for user: {}", userId);
            oAuthService.handleCallback(userId, code);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Facebook page connected successfully"
            ));
        } catch (RuntimeException ex) {
            log.error("Error handling Facebook callback: {}", ex.getMessage());
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
                            "error", "Failed to connect Facebook: " + ex.getMessage()
                    ));
        }
    }
}
