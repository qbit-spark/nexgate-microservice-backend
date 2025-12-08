package org.nextgate.nextgatebackend.e_social.posts_mng.repo;

import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostCommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostCommentRepository extends JpaRepository<PostCommentEntity, UUID> {

    // Get top-level comments for a post
    Page<PostCommentEntity> findByPostIdAndParentCommentIdIsNullAndIsDeletedFalse(UUID postId, Pageable pageable);

    // Get replies to a comment
    Page<PostCommentEntity> findByParentCommentIdAndIsDeletedFalse(UUID parentCommentId, Pageable pageable);

    // Get pinned comments
    List<PostCommentEntity> findByPostIdAndIsPinnedTrueAndIsDeletedFalse(UUID postId);

    // Count comments on a post
    long countByPostIdAndIsDeletedFalse(UUID postId);

    // Find comment by ID and not deleted
    Optional<PostCommentEntity> findByIdAndIsDeletedFalse(UUID id);

    // Check if user commented on post
    boolean existsByPostIdAndUserIdAndIsDeletedFalse(UUID postId, UUID userId);
}