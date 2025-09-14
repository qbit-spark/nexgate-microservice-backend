package org.nextgate.nextgatebackend.shops_mng_service.shops.rates.repo;

import org.nextgate.nextgatebackend.shops_mng_service.shops.rates.entity.ShopRatingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShopRatingRepo extends JpaRepository<ShopRatingEntity, UUID> {

    Optional<ShopRatingEntity> findByShopShopIdAndUserIdAndIsDeletedFalse(UUID shopId, UUID userId);

    List<ShopRatingEntity> findByShopShopIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID shopId);

    List<ShopRatingEntity> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);

    boolean existsByShopShopIdAndUserIdAndIsDeletedFalse(UUID shopId, UUID userId);

    List<ShopRatingEntity> findByShopShopIdAndIsDeletedFalse(UUID shopId);

    long countByShopShopIdAndIsDeletedFalse(UUID shopId);

    List<ShopRatingEntity> findByShopShopIdAndRatingValueAndIsDeletedFalse(UUID shopId, Integer ratingValue);
}