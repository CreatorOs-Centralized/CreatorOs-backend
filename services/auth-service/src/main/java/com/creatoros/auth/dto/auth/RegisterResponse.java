package com.creatoros.auth.dto.auth;

public record RegisterResponse(
        String userId,
        boolean emailVerified,
        String emailVerificationToken
) {
}
