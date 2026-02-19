package com.creatoros.auth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.creatoros.auth.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    @Query("select t from EmailVerificationToken t join fetch t.user u where t.id = :id")
    Optional<EmailVerificationToken> findWithUserById(@Param("id") UUID id);

    @Query("select t from EmailVerificationToken t where t.user.id = :userId and t.usedAt is null and t.expiresAt > :now")
    Optional<EmailVerificationToken> findActiveForUser(@Param("userId") String userId, @Param("now") Instant now);
}
