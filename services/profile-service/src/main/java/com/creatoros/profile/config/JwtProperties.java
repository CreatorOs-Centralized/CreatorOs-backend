package com.creatoros.profile.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "creatoros.security.jwt")
public class JwtProperties {

    /**
     * Shared HMAC secret used to validate access JWTs issued by auth-service.
     * Must be at least 32 bytes.
     */
    private String secret;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
