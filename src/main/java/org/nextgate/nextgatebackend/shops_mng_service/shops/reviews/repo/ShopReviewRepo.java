package org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.repo;


import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.entity.ShopReviewEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShopReviewRepo extends JpaRepository<ShopReviewEntity, UUID> {

    Optional<ShopReviewEntity> findByShopShopIdAndUserIdAndIsDeletedFalse(UUID shopId, UUID userId);

    boolean existsByShopShopIdAndUserIdAndIsDeletedFalse(UUID shopId, UUID userId);

    List<ShopReviewEntity> findByShopShopIdAndIsDeletedFalseAndStatusOrderByCreatedAtDesc(UUID shopId, ReviewStatus status);

    Page<ShopReviewEntity> findByShopShopIdAndIsDeletedFalseAndStatusOrderByCreatedAtDesc(UUID shopId, ReviewStatus status, Pageable pageable);

    List<ShopReviewEntity> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);

    Page<ShopReviewEntity> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByShopShopIdAndIsDeletedFalseAndStatus(UUID shopId, ReviewStatus status);
}