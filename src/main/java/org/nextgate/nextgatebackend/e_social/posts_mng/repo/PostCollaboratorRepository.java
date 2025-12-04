package org.nextgate.nextgatebackend.e_social.posts_mng.repo;

import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostCollaboratorEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.CollaboratorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostCollaboratorRepository extends JpaRepository<PostCollaboratorEntity, UUID> {

    List<PostCollaboratorEntity> findByPostId(UUID postId);

    List<PostCollaboratorEntity> findByPostIdAndStatus(UUID postId, CollaboratorStatus status);

    List<PostCollaboratorEntity> findByUserIdAndStatus(UUID userId, CollaboratorStatus status);

    Optional<PostCollaboratorEntity> findByPostIdAndUserId(UUID postId, UUID userId);

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    void deleteByPostId(UUID postId);
}