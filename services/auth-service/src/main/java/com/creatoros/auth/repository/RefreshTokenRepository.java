package com.creatoros.auth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.creatoros.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Query("select rt from RefreshToken rt join fetch rt.user u where rt.id = :id")
    Optional<RefreshToken> findWithUserById(@Param("id") UUID id);

    @Query("select rt from RefreshToken rt where rt.id = :id and rt.revokedAt is null and rt.expiresAt > :now")
    Optional<RefreshToken> findActiveById(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("update RefreshToken rt set rt.revokedAt = :revokedAt where rt.user.id = :userId and rt.revokedAt is null")
    int revokeAllActiveForUser(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);

    @Modifying
    @Query("update RefreshToken rt set rt.revokedAt = :revokedAt where rt.id = :id and rt.revokedAt is null")
    int revokeById(@Param("id") UUID id, @Param("revokedAt") Instant revokedAt);
}
