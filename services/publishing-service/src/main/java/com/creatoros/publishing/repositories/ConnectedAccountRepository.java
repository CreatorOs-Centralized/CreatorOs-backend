package com.creatoros.publishing.repositories;

import com.creatoros.publishing.entities.ConnectedAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConnectedAccountRepository extends JpaRepository<ConnectedAccount, UUID> {
    
    List<ConnectedAccount> findByUserId(UUID userId);
    
    Optional<ConnectedAccount> findByUserIdAndPlatform(UUID userId, String platform);
    
    List<ConnectedAccount> findByUserIdAndIsActiveTrue(UUID userId);
}
