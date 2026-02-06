package com.creatoros.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.creatoros.auth.model.LoginSession;
import com.creatoros.auth.model.User;
import com.creatoros.auth.repository.LoginSessionRepository;
import com.creatoros.auth.repository.RefreshTokenRepository;
import com.creatoros.auth.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private final LoginSessionRepository loginSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public SessionService(LoginSessionRepository loginSessionRepository, RefreshTokenRepository refreshTokenRepository, Clock clock) {
        this.loginSessionRepository = loginSessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    @Transactional
    public void recordSessionIfMissing(User user, AuthenticatedUser principal, HttpServletRequest request) {
        if (principal.sessionId() == null || principal.sessionId().isBlank()) {
            return;
        }
        Optional<LoginSession> existing = loginSessionRepository.findFirstByExternalSessionIdAndRevokedFalse(principal.sessionId());
        if (existing.isPresent()) {
            return;
        }

        LoginSession session = new LoginSession(UUID.randomUUID(), user, principal.sessionId(), Instant.now(clock));
        if (request != null) {
            session.setIpAddress(extractClientIp(request));
            session.setUserAgent(request.getHeader("User-Agent"));
        }
        loginSessionRepository.save(session);
    }

    @Transactional
    public void logout(AuthenticatedUser principal) {
        Instant now = Instant.now(clock);
        if (principal.sessionId() != null && !principal.sessionId().isBlank()) {
            loginSessionRepository.findFirstByExternalSessionIdAndRevokedFalse(principal.sessionId())
                    .ifPresent(s -> s.revoke(now));
        }
        refreshTokenRepository.revokeAllActiveForUser(principal.userId(), now);
    }

    private static String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }
}
