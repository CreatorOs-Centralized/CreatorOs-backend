package com.creatoros.auth.repository;

import java.time.Instant;
import java.util.UUID;

import com.creatoros.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Modifying
    @Query("update RefreshToken rt set rt.revoked = true, rt.revokedAt = :revokedAt where rt.user.id = :userId and rt.revoked = false")
    int revokeAllActiveForUser(@Param("userId") String userId, @Param("revokedAt") Instant revokedAt);
}
