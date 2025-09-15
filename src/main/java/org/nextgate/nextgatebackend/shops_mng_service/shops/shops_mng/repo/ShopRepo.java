package org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.repo;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
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
    Page<ShopEntity> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    List<ShopEntity> findByCategoryCategoryIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID categoryId);
    Page<ShopEntity> findByCategoryCategoryIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID categoryId, Pageable pageable);

    List<ShopEntity> findByOwner(AccountEntity owner);
    Page<ShopEntity> findByOwner(AccountEntity owner, Pageable pageable);
}