package org.nextgate.nextgatebackend.e_social.posts_mng.repo;

import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostShopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

public interface PostShopRepository extends JpaRepository<PostShopEntity, UUID> {

    List<PostShopEntity> findByPostId(UUID postId);

    List<PostShopEntity> findByShopId(UUID shopId);

    boolean existsByPostIdAndShopId(UUID postId, UUID shopId);

    void deleteByPostId(UUID postId);
}