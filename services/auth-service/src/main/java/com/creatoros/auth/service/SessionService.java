package com.creatoros.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.creatoros.auth.model.LoginSession;
import com.creatoros.auth.model.User;
import com.creatoros.auth.repository.LoginSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import com.creatoros.auth.util.HttpRequestUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private final LoginSessionRepository loginSessionRepository;
    private final Clock clock;

    public SessionService(LoginSessionRepository loginSessionRepository, Clock clock) {
        this.loginSessionRepository = loginSessionRepository;
        this.clock = clock;
    }

    @Transactional
    public UUID recordLoginAttempt(User user, HttpServletRequest request, boolean isSuccess, String failureReason) {
        Instant now = Instant.now(clock);

        UUID sessionId = UUID.randomUUID();
        LoginSession session = new LoginSession(sessionId, user, now);
        session.setIpAddress(HttpRequestUtil.extractClientIp(request));
        session.setUserAgent(HttpRequestUtil.extractUserAgent(request));
        if (!isSuccess) {
            session.markFailure(failureReason);
        }
        loginSessionRepository.save(session);
        return sessionId;
    }

    @Transactional
    public void markLoggedOut(String sessionIdRaw) {
        if (sessionIdRaw == null || sessionIdRaw.isBlank()) {
            return;
        }

        UUID sessionId;
        try {
            sessionId = UUID.fromString(sessionIdRaw.trim());
        } catch (IllegalArgumentException ex) {
            return;
        }

        Instant now = Instant.now(clock);
        loginSessionRepository.findById(sessionId)
                .ifPresent(s -> s.markLoggedOut(now));
    }
}
