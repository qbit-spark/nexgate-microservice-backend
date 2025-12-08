package org.nextgate.nextgatebackend.e_social.posts_mng.repo;

import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.PostStatus;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.PostType;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.PostVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<PostEntity, UUID> {

    Optional<PostEntity> findByIdAndIsDeletedFalse(UUID id);

    Optional<PostEntity> findByAuthorIdAndStatusAndIsDeletedFalse(UUID authorId, PostStatus status);

    List<PostEntity> findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID authorId);

    Page<PostEntity> findByAuthorIdAndIsDeletedFalse(UUID authorId, Pageable pageable);

    Page<PostEntity> findByAuthorIdAndStatusAndIsDeletedFalse(UUID authorId, PostStatus status, Pageable pageable);

    Page<PostEntity> findByStatusAndIsDeletedFalse(PostStatus status, Pageable pageable);

    Page<PostEntity> findByPostTypeAndStatusAndIsDeletedFalse(PostType postType, PostStatus status, Pageable pageable);

    List<PostEntity> findByAuthorIdAndStatusAndScheduledAtBeforeAndIsDeletedFalse(UUID authorId, PostStatus status, LocalDateTime scheduledAt);

    long countByAuthorIdAndIsDeletedFalse(UUID authorId);

    boolean existsByIdAndAuthorId(UUID id, UUID authorId);

    List<PostEntity>findByAuthorIdAndIsDeletedFalseAndStatus(UUID authorId, PostStatus status);

    List<PostEntity> findByAuthorIdInAndIsDeletedFalseAndStatusOrderByCreatedAtDesc(List<UUID> authorIds, PostStatus status);

   List<PostEntity>findByIsDeletedFalseAndStatusAndVisibilityOrderByCreatedAtDesc(PostStatus status, PostVisibility visibility);

    @Modifying
    @Query("UPDATE PostEntity p SET p.quotesCount = p.quotesCount + 1, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :postId")
    void incrementQuotesCount(@Param("postId") UUID postId);
}