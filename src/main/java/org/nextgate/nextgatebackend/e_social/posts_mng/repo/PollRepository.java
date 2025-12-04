package org.nextgate.nextgatebackend.e_social.posts_mng.repo;

import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PollEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PollRepository extends JpaRepository<PollEntity, UUID> {

    Optional<PollEntity> findByPostId(UUID postId);

    boolean existsByPostId(UUID postId);

    void deleteByPostId(UUID postId);
}