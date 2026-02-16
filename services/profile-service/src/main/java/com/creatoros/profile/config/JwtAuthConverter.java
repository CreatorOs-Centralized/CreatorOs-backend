package com.creatoros.profile.config;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    /**
     * Keycloak JWT expectations for this service:
     * - This service is a pure OAuth2 Resource Server (validates JWTs), not an auth server.
     * - userId comes from {@code sub}.
     * - Roles are accepted ONLY from:
     *   1) {@code realm_access.roles}
     *   2) {@code resource_access[azp].roles} where {@code azp} is the token's "authorized party" (client id)
     *
     * This avoids accidental role leakage from other Keycloak clients present under {@code resource_access}.
     */

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String userId = jwt.getSubject();
        String username = firstNonBlank(
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("username"),
                jwt.getClaimAsString("name")
        );
        String email = jwt.getClaimAsString("email");
        String sessionId = firstNonBlank(jwt.getClaimAsString("sid"), jwt.getId());

        Set<String> roles = extractAllowedRoles(jwt);
        Collection<GrantedAuthority> authorities = roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        ProfileAuthenticatedUser principal = new ProfileAuthenticatedUser(userId, username, email, sessionId, roles);
        return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
    }

    private static Set<String> extractAllowedRoles(Jwt jwt) {
        LinkedHashSet<String> roles = new LinkedHashSet<>();

        roles.addAll(extractRolesFromClaimPath(jwt.getClaims(), "realm_access", "roles"));

        String azp = jwt.getClaimAsString("azp");
        if (azp != null && !azp.isBlank()) {
            Object resourceAccess = jwt.getClaims().get("resource_access");
            if (resourceAccess instanceof Map<?, ?> resourceAccessMap) {
                Object clientAccess = resourceAccessMap.get(azp);
                if (clientAccess instanceof Map<?, ?> clientAccessMap) {
                    roles.addAll(extractRolesFromClaimPath(clientAccessMap, "roles"));
                }
            }
        }

        return Collections.unmodifiableSet(roles);
    }

    private static Set<String> extractRolesFromClaimPath(Map<?, ?> root, String... path) {
        Object current = root;
        for (int i = 0; i < path.length; i++) {
            if (!(current instanceof Map<?, ?> currentMap)) {
                return Collections.emptySet();
            }
            current = currentMap.get(path[i]);
        }
        if (!(current instanceof Collection<?> roleCollection)) {
            return Collections.emptySet();
        }
        return roleCollection.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) return null;
        for (String c : candidates) {
            if (c != null && !c.isBlank()) return c;
        }
        return null;
    }
}
