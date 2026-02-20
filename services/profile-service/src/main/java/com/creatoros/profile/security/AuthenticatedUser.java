package com.creatoros.profile.security;

import java.util.Set;

public record AuthenticatedUser(
        String userId,
        String email,
        Set<String> roles
) {
}
