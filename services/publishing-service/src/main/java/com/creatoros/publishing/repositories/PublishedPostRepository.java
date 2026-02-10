package com.creatoros.publishing.repositories;

import com.creatoros.publishing.entities.PublishedPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PublishedPostRepository extends JpaRepository<PublishedPost, UUID> {
    
    Optional<PublishedPost> findByPublishJobId(UUID publishJobId);
    
    List<PublishedPost> findByConnectedAccountId(UUID connectedAccountId);
    
    List<PublishedPost> findByPlatform(String platform);
}
