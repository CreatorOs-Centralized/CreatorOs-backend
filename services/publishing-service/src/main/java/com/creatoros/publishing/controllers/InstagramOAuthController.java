package com.creatoros.publishing.controllers;

import com.creatoros.publishing.services.InstagramOAuthService;
import com.creatoros.publishing.utils.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class InstagramOAuthController {

    private final InstagramOAuthService oAuthService;

    @GetMapping("/oauth/instagram/login")
    public String login() {
        String userId = UserContextUtil.getCurrentUserId().toString();
        log.info("Generating Instagram OAuth URL for user {}", userId);
        return oAuthService.buildAuthorizationUrl(userId);
    }

    @GetMapping("/oauth/instagram/callback")
    public ResponseEntity<?> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false, name = "error") String error,
            @RequestParam(required = false, name = "error_description") String errorDescription) {
        try {
            if (error != null && !error.isBlank()) {
                String reason = (errorDescription != null && !errorDescription.isBlank()) ? errorDescription : error;
                throw new RuntimeException("Instagram authorization canceled: " + reason);
            }
            if (code == null || code.isBlank()) {
                throw new RuntimeException("Missing authorization code");
            }

            String userId = oAuthService.resolveState(state);
            log.info("Received Instagram OAuth callback for user {}", userId);
            oAuthService.handleCallback(userId, code);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Instagram account connected successfully"
            ));
        } catch (RuntimeException ex) {
            log.error("Error handling Instagram callback: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "error", ex.getMessage()
                    ));
        } catch (Exception ex) {
            log.error("Unexpected Instagram callback error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to connect Instagram: " + ex.getMessage()
                    ));
        }
    }

                @GetMapping("/instagram/refresh")
                public ResponseEntity<?> refreshLongLivedToken(
                    @RequestParam(required = false) UUID accountId) {
                try {
                    String userId = UserContextUtil.getCurrentUserId().toString();
                    Map<String, Object> refreshed = accountId != null
                        ? oAuthService.refreshLongLivedToken(userId, accountId)
                        : oAuthService.refreshLongLivedToken(userId);

                    return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Instagram long-lived token refreshed successfully",
                        "data", refreshed
                    ));
                } catch (RuntimeException ex) {
                    log.error("Error refreshing Instagram token: {}", ex.getMessage());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                            "success", false,
                            "error", ex.getMessage()
                        ));
                } catch (Exception ex) {
                    log.error("Unexpected Instagram refresh error", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                            "success", false,
                            "error", "Failed to refresh Instagram token: " + ex.getMessage()
                        ));
                }
                }
}
