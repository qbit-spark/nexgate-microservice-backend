package org.nextgate.nextgatebackend.e_social.posts_mng.repo;

import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostUserMentionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostUserMentionRepository extends JpaRepository<PostUserMentionEntity, UUID> {

    List<PostUserMentionEntity> findByPostId(UUID postId);

    List<PostUserMentionEntity> findByMentionedUserId(UUID mentionedUserId);

    Page<PostUserMentionEntity> findByMentionedUserId(UUID mentionedUserId, Pageable pageable);

    boolean existsByPostIdAndMentionedUserId(UUID postId, UUID mentionedUserId);

    void deleteByPostId(UUID postId);
}