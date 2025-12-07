package org.nextgate.nextgatebackend.e_social.interactions.repo;

import org.nextgate.nextgatebackend.e_social.interactions.entity.PostBookmarkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostBookmarkRepository extends JpaRepository<PostBookmarkEntity, UUID> {

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    Optional<PostBookmarkEntity> findByPostIdAndUserId(UUID postId, UUID userId);

    long countByPostId(UUID postId);

    void deleteByPostId(UUID postId);
}