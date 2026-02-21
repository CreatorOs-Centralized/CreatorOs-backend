package com.creatoros.auth.config;

import java.time.Duration;

import com.creatoros.auth.util.DurationUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "creatoros.security.jwt")
public class JwtProperties {

    /**
     * HMAC secret for signing/validating access JWTs (HS256). Must be >= 32 bytes.
     * Provided via env var JWT_SECRET.
     */
    private String secret;

    /**
     * Access token expiration (seconds or ISO-8601 duration). Provided via env var ACCESS_TOKEN_EXPIRATION.
     */
    private String accessTokenExpiration;

    /**
     * Refresh token expiration (seconds or ISO-8601 duration). Provided via env var REFRESH_TOKEN_EXPIRATION.
     */
    private String refreshTokenExpiration;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public void setAccessTokenExpiration(String accessTokenExpiration) {
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public String getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public void setRefreshTokenExpiration(String refreshTokenExpiration) {
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public Duration accessTokenDuration() {
        return DurationUtil.parseDurationSecondsOrIso(accessTokenExpiration, Duration.ofMinutes(15));
    }

    public Duration refreshTokenDuration() {
        return DurationUtil.parseDurationSecondsOrIso(refreshTokenExpiration, Duration.ofDays(7));
    }
}
