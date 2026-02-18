package com.creatoros.profile.config;

import java.util.Set;

/**
 * Authenticated user principal for profile service.
 * Extracted from Keycloak JWT bearer token.
 */
public class ProfileAuthenticatedUser {
    private final String userId;
    private final String username;
    private final String email;
    private final String sessionId;
    private final Set<String> roles;

    public ProfileAuthenticatedUser(String userId, String username, String email, String sessionId, Set<String> roles) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.sessionId = sessionId;
        this.roles = roles;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Set<String> getRoles() {
        return roles;
    }

    @Override
    public String toString() {
        return "ProfileAuthenticatedUser{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", roles=" + roles +
                '}';
    }
}
