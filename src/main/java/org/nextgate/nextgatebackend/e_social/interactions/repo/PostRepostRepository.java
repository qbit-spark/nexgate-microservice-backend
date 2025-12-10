package org.nextgate.nextgatebackend.e_social.interactions.repo;

import org.nextgate.nextgatebackend.e_social.interactions.entity.PostRepostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepostRepository extends JpaRepository<PostRepostEntity, UUID> {

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    Optional<PostRepostEntity> findByPostIdAndUserId(UUID postId, UUID userId);

    long countByPostId(UUID postId);

    void deleteByPostId(UUID postId);

    List<PostRepostEntity> findAllByUserId(UUID userId);

    List<PostRepostEntity>findByUserIdIn(List<UUID> userIds);

    Page<PostRepostEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}