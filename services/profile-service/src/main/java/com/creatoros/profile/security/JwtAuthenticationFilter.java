package com.creatoros.profile.security;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length()).trim();
            if (!token.isBlank()) {
                try {
                    Jws<Claims> jws = jwtUtil.parseAndValidate(token);
                    Claims claims = jws.getPayload();

                    String userId = claims.get(JwtUtil.CLAIM_USER_ID, String.class);
                    String email = claims.get(JwtUtil.CLAIM_EMAIL, String.class);
                    Set<String> roles = extractRoles(claims.get(JwtUtil.CLAIM_ROLES));

                    Collection<? extends GrantedAuthority> authorities = roles.stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(r -> !r.isEmpty())
                            .map(JwtAuthenticationFilter::asAuthority)
                            .toList();

                    AuthenticatedUser principal = new AuthenticatedUser(userId, email, roles);

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            authorities
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (JwtException | IllegalArgumentException ex) {
                    SecurityContextHolder.clearContext();
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private static SimpleGrantedAuthority asAuthority(String role) {
        String normalized = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return new SimpleGrantedAuthority(normalized);
    }

    private static Set<String> extractRoles(Object claimValue) {
        Set<String> roles = new LinkedHashSet<>();
        if (claimValue == null) {
            return roles;
        }
        if (claimValue instanceof List<?> list) {
            for (Object o : list) {
                if (o != null) {
                    roles.add(String.valueOf(o));
                }
            }
            return roles;
        }
        roles.add(String.valueOf(claimValue));
        return roles;
    }
}
