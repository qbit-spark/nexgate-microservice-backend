package org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.repo;

import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.enums.ShopStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShopRepo extends JpaRepository<ShopEntity, UUID> {

    // EXISTENCE CHECKS
    boolean existsByShopNameAndIsDeletedFalse(String shopName);

    // NON-PAGEABLE LISTS (return List)
    List<ShopEntity> findByIsDeletedFalseOrderByCreatedAtDesc();
    List<ShopEntity> findByIsDeletedFalseAndStatusOrderByCreatedAtDesc(ShopStatus status);
    List<ShopEntity> findByIsDeletedFalseAndIsFeaturedTrueOrderByCreatedAtDesc();

    // PAGEABLE LISTS (return Page)
    Page<ShopEntity> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);
    Page<ShopEntity> findByIsDeletedFalseAndStatusOrderByCreatedAtDesc(ShopStatus status, Pageable pageable);
    Page<ShopEntity> findByIsDeletedFalseAndIsFeaturedTrueOrderByCreatedAtDesc(Pageable pageable);

    // SEARCH
    Page<ShopEntity> findByIsDeletedFalseAndShopNameContainingIgnoreCaseOrderByCreatedAtDesc(String shopName, Pageable pageable);
}