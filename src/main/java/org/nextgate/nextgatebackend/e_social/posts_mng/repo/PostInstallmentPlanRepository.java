package org.nextgate.nextgatebackend.e_social.posts_mng.repo;

import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostInstallmentPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

public interface PostInstallmentPlanRepository extends JpaRepository<PostInstallmentPlanEntity, UUID> {

    List<PostInstallmentPlanEntity> findByPostId(UUID postId);

    List<PostInstallmentPlanEntity> findByPlanId(UUID planId);

    boolean existsByPostIdAndPlanId(UUID postId, UUID planId);

    void deleteByPostId(UUID postId);
}