package org.nextgate.nextgatebackend.e_social.interactions.repo;

import org.nextgate.nextgatebackend.e_social.interactions.entity.PostViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostViewRepository extends JpaRepository<PostViewEntity, UUID> {

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    Optional<PostViewEntity> findByPostIdAndUserId(UUID postId, UUID userId);

    long countByPostId(UUID postId);

    void deleteByPostId(UUID postId);
}