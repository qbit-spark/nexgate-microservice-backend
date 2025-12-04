package org.nextgate.nextgatebackend.e_social.posts_mng.repo;

import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PostEventRepository extends JpaRepository<PostEventEntity, UUID> {

    List<PostEventEntity> findByPostId(UUID postId);

    List<PostEventEntity> findByEventId(UUID eventId);

    boolean existsByPostIdAndEventId(UUID postId, UUID eventId);

    void deleteByPostId(UUID postId);
}