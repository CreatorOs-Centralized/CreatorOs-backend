package com.creatoros.auth.security;

import java.util.Set;

public record AuthenticatedUser(
        String userId,
        String username,
        String email,
        String sessionId,
        Set<String> roles
) {
}
