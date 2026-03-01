package com.creatoros.auth.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record GoogleOAuthRequest(
        @NotBlank String code,
        @NotBlank String redirectUri,
        String codeVerifier
) {
}
