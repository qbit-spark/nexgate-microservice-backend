package org.nextgate.nextgatebackend.installment_purchase.repo;

import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentPlanEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstallmentPlanRepo extends JpaRepository<InstallmentPlanEntity, UUID> {

    // Find all plans for a product
    List<InstallmentPlanEntity> findByProductOrderByDisplayOrderAsc(ProductEntity product);

    // Find active plans for a product
    List<InstallmentPlanEntity> findByProductAndIsActiveTrueOrderByDisplayOrderAsc(ProductEntity product);

    // Find featured plan for a product
    InstallmentPlanEntity findByProductAndIsFeaturedTrue(ProductEntity product);

    // Find all plans for a shop
    List<InstallmentPlanEntity> findByShopOrderByCreatedAtDesc(ShopEntity shop);

    // Count active plans for product
    long countByProductAndIsActiveTrue(ProductEntity product);
}