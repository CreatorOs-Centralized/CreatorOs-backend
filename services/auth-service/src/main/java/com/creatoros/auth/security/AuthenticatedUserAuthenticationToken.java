package com.creatoros.auth.security;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Resource-server authentication token that carries the validated {@link Jwt} and the derived
 * {@link AuthenticatedUser} principal.
 *
 * <p>We intentionally do not use username/password based authentication types here.
 */
public final class AuthenticatedUserAuthenticationToken extends AbstractAuthenticationToken {

    private final AuthenticatedUser principal;
    private final Jwt jwt;

    public AuthenticatedUserAuthenticationToken(
            AuthenticatedUser principal,
            Jwt jwt,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.principal = principal;
        this.jwt = jwt;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        // No passwords/credentials are stored; the JWT itself is the credential material.
        return jwt;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public Jwt getJwt() {
        return jwt;
    }
}
