package com.creatoros.auth.service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import com.creatoros.auth.model.User;
import com.creatoros.auth.security.AuthenticatedUser;
import org.springframework.stereotype.Service;

@Service
public class KeycloakSyncService {

    public KeycloakUserSnapshot snapshotFromToken(AuthenticatedUser principal) {
        Set<String> roles = principal.roles() == null ? Set.of() : new LinkedHashSet<>(principal.roles());
        return new KeycloakUserSnapshot(
                principal.userId(),
                principal.username(),
                principal.email(),
                roles,
                Instant.now()
        );
    }

    public void applyToUser(User user, KeycloakUserSnapshot snapshot) {
        user.setUsername(snapshot.username());
        user.setEmail(snapshot.email());
        user.setEnabled(true);
    }

    public record KeycloakUserSnapshot(
            String userId,
            String username,
            String email,
            Set<String> roles,
            Instant observedAt
    ) {
    }
}
