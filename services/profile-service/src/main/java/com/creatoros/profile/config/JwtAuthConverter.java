package com.creatoros.profile.config;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
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

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
    String userId = firstNonBlank(jwt.getClaimAsString("user_id"), jwt.getSubject());
        String username = firstNonBlank(
        jwt.getClaimAsString("username"),
        jwt.getClaimAsString("preferred_username"),
        jwt.getClaimAsString("name"),
        jwt.getClaimAsString("email")
        );
        String email = jwt.getClaimAsString("email");
        String sessionId = firstNonBlank(jwt.getClaimAsString("sid"), jwt.getId());

    Set<String> roles = extractRoles(jwt);
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

    private static Set<String> extractRoles(Jwt jwt) {
        Object rawRoles = jwt.getClaims().get("roles");
        if (rawRoles == null) {
            return Collections.emptySet();
        }

        LinkedHashSet<String> roles = new LinkedHashSet<>();
        if (rawRoles instanceof Collection<?> roleCollection) {
            roleCollection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .forEach(roles::add);
        } else {
            String roleString = rawRoles.toString();
            for (String r : roleString.split(",")) {
                String trimmed = r.trim();
                if (!trimmed.isEmpty()) {
                    roles.add(trimmed);
                }
            }
        }
        return Collections.unmodifiableSet(roles);
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) return null;
        for (String c : candidates) {
            if (c != null && !c.isBlank()) return c;
        }
        return null;
    }
}
