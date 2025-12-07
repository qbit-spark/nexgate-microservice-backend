package org.nextgate.nextgatebackend.e_social.interactions.repo;

import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostRepostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepostRepository extends JpaRepository<PostRepostEntity, UUID> {

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    Optional<PostRepostEntity> findByPostIdAndUserId(UUID postId, UUID userId);

    long countByPostId(UUID postId);

    void deleteByPostId(UUID postId);
}