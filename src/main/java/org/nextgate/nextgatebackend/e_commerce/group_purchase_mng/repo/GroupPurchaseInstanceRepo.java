package org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.repo;


import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.enums.GroupStatus;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Fetch group with all required relationships for notifications.
     * Eagerly loads: shop, shop.owner, product, initiator
     */
    @Query("""
        SELECT g FROM GroupPurchaseInstanceEntity g
        LEFT JOIN FETCH g.shop s
        LEFT JOIN FETCH s.owner
        LEFT JOIN FETCH g.product
        LEFT JOIN FETCH g.initiator
        WHERE g.groupInstanceId = :groupId
    """)
    Optional<GroupPurchaseInstanceEntity> findByIdWithRelations(@Param("groupId") UUID groupId);

}