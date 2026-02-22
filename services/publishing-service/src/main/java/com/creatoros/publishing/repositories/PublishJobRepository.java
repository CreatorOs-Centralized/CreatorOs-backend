package com.creatoros.publishing.repositories;

import com.creatoros.publishing.entities.PublishJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PublishJobRepository extends JpaRepository<PublishJob, UUID> {
    
    List<PublishJob> findByUserId(UUID userId);
    
    Optional<PublishJob> findByIdempotencyKey(String idempotencyKey);
    
    List<PublishJob> findByStatus(String status);
    
    List<PublishJob> findByUserIdAndStatus(UUID userId, String status);
}
