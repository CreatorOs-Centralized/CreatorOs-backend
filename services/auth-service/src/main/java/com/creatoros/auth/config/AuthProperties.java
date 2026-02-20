package com.creatoros.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "creatoros.auth")
public class AuthProperties {

    /**
     * When true, endpoints may include raw verification/reset tokens in responses.
     * MUST be false in production.
     */
    private boolean debugTokenResponse;

    /**
     * Frontend base URL used to build verification links.
     * Example: http://localhost:5173
     */
    private String frontendBaseUrl = "http://localhost:5173";

    /**
     * Default "From" address for outbound emails.
     */
    private String mailFrom = "no-reply@creatoros.local";

    public boolean isDebugTokenResponse() {
        return debugTokenResponse;
    }

    public void setDebugTokenResponse(boolean debugTokenResponse) {
        this.debugTokenResponse = debugTokenResponse;
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }
}
