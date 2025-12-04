package org.nextgate.nextgatebackend.e_social.posts_mng.repo;

import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostBuyTogetherGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostBuyTogetherGroupRepository extends JpaRepository<PostBuyTogetherGroupEntity, UUID> {

    List<PostBuyTogetherGroupEntity> findByPostId(UUID postId);

    List<PostBuyTogetherGroupEntity> findByGroupId(UUID groupId);

    boolean existsByPostIdAndGroupId(UUID postId, UUID groupId);

    void deleteByPostId(UUID postId);
}