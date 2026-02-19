package com.creatoros.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "creatoros.auth")
public class AuthProperties {

    /**
     * When true, endpoints may include raw verification/reset tokens in responses.
     * MUST be false in production.
     */
    private boolean debugTokenResponse;

    public boolean isDebugTokenResponse() {
        return debugTokenResponse;
    }

    public void setDebugTokenResponse(boolean debugTokenResponse) {
        this.debugTokenResponse = debugTokenResponse;
    }
}
