package org.nextgate.nextgatebackend.e_social.posts_mng.repo;

import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostShopMentionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

public interface PostShopMentionRepository extends JpaRepository<PostShopMentionEntity, UUID> {

    List<PostShopMentionEntity> findByPostId(UUID postId);

    List<PostShopMentionEntity> findByMentionedShopId(UUID mentionedShopId);

    Page<PostShopMentionEntity> findByMentionedShopId(UUID mentionedShopId, Pageable pageable);

    boolean existsByPostIdAndMentionedShopId(UUID postId, UUID mentionedShopId);

    void deleteByPostId(UUID postId);
}