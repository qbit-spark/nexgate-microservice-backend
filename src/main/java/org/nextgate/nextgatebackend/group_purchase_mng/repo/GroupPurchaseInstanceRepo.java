package org.nextgate.nextgatebackend.group_purchase_mng.repo;


import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.enums.GroupStatus;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupPurchaseInstanceRepo extends JpaRepository<GroupPurchaseInstanceEntity, UUID> {

    // Find by group code
    Optional<GroupPurchaseInstanceEntity> findByGroupCode(String groupCode);

    // Find active groups for a product
    List<GroupPurchaseInstanceEntity> findByProductAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(
            ProductEntity product, GroupStatus status);

    // Find all groups for a product
    List<GroupPurchaseInstanceEntity> findByProductAndIsDeletedFalseOrderByCreatedAtDesc(
            ProductEntity product);

    // Find groups by shop
    List<GroupPurchaseInstanceEntity> findByShopAndIsDeletedFalseOrderByCreatedAtDesc(
            ShopEntity shop);

    // Find expired open groups (for scheduled job)
    List<GroupPurchaseInstanceEntity> findByStatusAndExpiresAtBeforeAndIsDeletedFalse(
            GroupStatus status, LocalDateTime expiresAt);

    // Find groups by status
    List<GroupPurchaseInstanceEntity> findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(
            GroupStatus status);
}