package com.creatoros.publishing.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class UserContextUtil {

    private UserContextUtil() {
    }

    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Unauthenticated request");
        }

        Object principal = authentication.getPrincipal();
        String userId = principal.toString();
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid authenticated userId");
        }
    }
}