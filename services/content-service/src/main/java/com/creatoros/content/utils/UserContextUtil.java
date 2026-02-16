package com.creatoros.content.utils;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * User Context Utility
 * 
 * Extracts authenticated user ID from X-User-Id header injected by API Gateway.
 */
public final class UserContextUtil {

    private UserContextUtil() {
    }

    /**
     * Get the current authenticated user's ID from request header
     * 
     * @return User ID from X-User-Id header
     * @throws IllegalArgumentException if header is missing
     */
    public static UUID getCurrentUserId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new RuntimeException("No request context found");
        }
        
        String userId = attrs.getRequest().getHeader("X-User-Id");
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("X-User-Id header is required");
        }
        
        return UUID.fromString(userId);
    }
}
