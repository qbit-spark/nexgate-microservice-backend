package org.nextgate.nextgatebackend.e_social.posts_mng.repo;

import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

public interface PostProductRepository extends JpaRepository<PostProductEntity, UUID> {

    List<PostProductEntity> findByPostId(UUID postId);

    List<PostProductEntity> findByProductId(UUID productId);

    boolean existsByPostIdAndProductId(UUID postId, UUID productId);

    void deleteByPostId(UUID postId);
}