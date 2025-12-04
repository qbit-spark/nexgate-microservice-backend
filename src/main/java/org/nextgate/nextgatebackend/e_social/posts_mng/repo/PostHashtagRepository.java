package org.nextgate.nextgatebackend.e_social.posts_mng.repo;

import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostHashtagEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostHashtagRepository extends JpaRepository<PostHashtagEntity, UUID> {

    List<PostHashtagEntity> findByPostId(UUID postId);

    List<PostHashtagEntity> findByHashtag(String hashtag);

    Page<PostHashtagEntity> findByHashtag(String hashtag, Pageable pageable);

    long countByHashtag(String hashtag);

    void deleteByPostId(UUID postId);
}