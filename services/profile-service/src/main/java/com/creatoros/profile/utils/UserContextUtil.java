package com.creatoros.profile.utils;

import java.util.UUID;

/**
 * User Context Utility
 * 
 * Utility class for retrieving the current authenticated user's context.
 * 
 * ⚠️ TEMPORARY STUB IMPLEMENTATION
 * This will be replaced with proper JWT token extraction from SecurityContextHolder
 * once authentication service is integrated.
 */
public class UserContextUtil {

    // TEMP: Mock user ID for development
    private static final UUID TEMP_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    /**
     * Get the current authenticated user's ID
     * 
     * TODO: Replace with proper JWT token extraction
     * Example implementation:
     * <pre>
     * Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
     * JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
     * return UUID.fromString(token.getTokenAttributes().get("userId").toString());
     * </pre>
     * 
     * @return User ID of the authenticated user
     */
    public static UUID getCurrentUserId() {
        // STUB: Returns hardcoded UUID for development
        return TEMP_USER_ID;
    }

    /**
     * Get the current authenticated user's username
     * 
     * TODO: Implement proper extraction from authentication context
     * 
     * @return Username of the authenticated user
     */
    public static String getCurrentUsername() {
        // STUB: Replace with actual implementation
        return "test-user";
    }

    /**
     * Check if a user is authenticated
     * 
     * TODO: Implement proper authentication check
     * 
     * @return true if user is authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        // STUB: Replace with actual implementation
        return true;
    }
}
