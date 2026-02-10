package com.creatoros.publishing.repositories;

import com.creatoros.publishing.entities.PublishJobAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PublishJobAttemptRepository extends JpaRepository<PublishJobAttempt, UUID> {
    
    List<PublishJobAttempt> findByPublishJobId(UUID publishJobId);
    
    List<PublishJobAttempt> findByPublishJobIdAndStatus(UUID publishJobId, String status);
}
