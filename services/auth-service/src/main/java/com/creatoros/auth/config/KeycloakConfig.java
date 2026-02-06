package com.creatoros.auth.config;

/**
 * Intentionally left empty.
 *
 * This module integrates with Keycloak strictly as an OAuth2 Resource Server
 * (JWT validation via issuer-uri). It does not call Keycloak admin/userinfo APIs.
 */
public final class KeycloakConfig {
    private KeycloakConfig() {
    }
}
