package org.nextgate.nextgatebackend.e_social.posts_mng.repo;

import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

public interface PostLinkRepository extends JpaRepository<PostLinkEntity, UUID> {

    Optional<PostLinkEntity> findByPostId(UUID postId);

    Optional<PostLinkEntity> findByShortCode(String shortCode);

    boolean existsByPostId(UUID postId);

    void deleteByPostId(UUID postId);
}