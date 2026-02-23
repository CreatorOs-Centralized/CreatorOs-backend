package com.creatoros.publishing.repositories;

import com.creatoros.publishing.entities.PublishedPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PublishedPostRepository extends JpaRepository<PublishedPost, UUID> {

    @Query(value = """
        SELECT pp.*
        FROM published_posts pp
        JOIN connected_accounts ca ON ca.id = pp.connected_account_id
        WHERE ca.user_id = :userId
        ORDER BY pp.created_at DESC
        """, nativeQuery = true)
    List<PublishedPost> findAllByUserId(UUID userId);

    @Query(value = """
        SELECT pp.*
        FROM published_posts pp
        JOIN connected_accounts ca ON ca.id = pp.connected_account_id
        WHERE pp.id = :postId AND ca.user_id = :userId
        """, nativeQuery = true)
    Optional<PublishedPost> findByIdAndUserId(UUID postId, UUID userId);
    
    Optional<PublishedPost> findByPublishJobId(UUID publishJobId);

        @Query(value = """
                        SELECT pp.*
                        FROM published_posts pp
                        JOIN connected_accounts ca ON ca.id = pp.connected_account_id
                        WHERE pp.connected_account_id = :connectedAccountId
                            AND ca.user_id = :userId
                        ORDER BY pp.created_at DESC
                        """, nativeQuery = true)
        List<PublishedPost> findByConnectedAccountIdAndUserId(UUID connectedAccountId, UUID userId);
    
    List<PublishedPost> findByConnectedAccountId(UUID connectedAccountId);

        @Query(value = """
                        SELECT pp.*
                        FROM published_posts pp
                        JOIN connected_accounts ca ON ca.id = pp.connected_account_id
                        WHERE LOWER(pp.platform) = LOWER(:platform)
                            AND ca.user_id = :userId
                        ORDER BY pp.created_at DESC
                        """, nativeQuery = true)
        List<PublishedPost> findByPlatformAndUserId(String platform, UUID userId);
    
    List<PublishedPost> findByPlatform(String platform);
}
