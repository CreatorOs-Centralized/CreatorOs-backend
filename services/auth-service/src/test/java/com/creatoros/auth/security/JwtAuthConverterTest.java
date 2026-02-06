package com.creatoros.auth.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthConverterTest {

    private final JwtAuthConverter converter = new JwtAuthConverter();

    @Test
    void convertsJwtToAuthenticatedUserWithRealmRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "user-123")
                .claim("preferred_username", "alice")
                .claim("email", "alice@example.com")
                .claim("sid", "kc-session-1")
                .claim("realm_access", Map.of("roles", List.of("admin", "user")))
            .claim("azp", "auth-service")
            .claim("resource_access", Map.of(
                "auth-service", Map.of("roles", List.of("svc")),
                "other-client", Map.of("roles", List.of("leak"))
            ))
                .issuedAt(Instant.now().minusSeconds(10))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        Authentication auth = converter.convert(jwt);

        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        AuthenticatedUser principal = (AuthenticatedUser) auth.getPrincipal();

        assertThat(principal.userId()).isEqualTo("user-123");
        assertThat(principal.username()).isEqualTo("alice");
        assertThat(principal.email()).isEqualTo("alice@example.com");
        assertThat(principal.sessionId()).isEqualTo("kc-session-1");
        assertThat(principal.roles()).containsExactlyInAnyOrder("admin", "user", "svc");
        assertThat(principal.roles()).doesNotContain("leak");

        assertThat(auth.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .contains("ROLE_admin", "ROLE_user", "ROLE_svc");
    }
}
