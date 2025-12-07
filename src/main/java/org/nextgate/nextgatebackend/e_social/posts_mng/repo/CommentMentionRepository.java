package org.nextgate.nextgatebackend.e_social.posts_mng.repo;

import org.nextgate.nextgatebackend.e_social.posts_mng.entity.CommentMentionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentMentionRepository extends JpaRepository<CommentMentionEntity, UUID> {

    List<CommentMentionEntity> findByCommentId(UUID commentId);

    void deleteByCommentId(UUID commentId);
}