package com.creatoros.profile.utils;

import com.creatoros.profile.config.ProfileAuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * User Context Utility
 * 
 * Extracts authenticated user ID from JWT security context.
 */
@Component
public class UserContextUtil {

    /**
     * Get the current authenticated user's ID from security context
     * 
     * @return User ID from JWT subject
     * @throws IllegalArgumentException if no authenticated user found
     */
    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("No authenticated user found");
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof ProfileAuthenticatedUser user) {
            return UUID.fromString(user.getUserId());
        }
        
        throw new IllegalArgumentException("Invalid principal type: " + (principal != null ? principal.getClass() : "null"));
    }
}

