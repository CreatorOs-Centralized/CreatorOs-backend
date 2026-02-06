package com.creatoros.auth.repository;

import java.util.Optional;
import java.util.UUID;

import com.creatoros.auth.model.LoginSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginSessionRepository extends JpaRepository<LoginSession, UUID> {
    Optional<LoginSession> findFirstByExternalSessionIdAndRevokedFalse(String externalSessionId);
}
