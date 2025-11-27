package org.nextgate.nextgatebackend.e_commerce.order_mng_service.repo;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.entity.ProductOrderEntity;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.enums.ProductOrderStatus;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductOrderRepository extends JpaRepository<ProductOrderEntity, UUID> {

    Optional<ProductOrderEntity> findByOrderNumber(String orderNumber);

    Optional<ProductOrderEntity> findByCheckoutSessionId(UUID checkoutSessionId);

    List<ProductOrderEntity> findByBuyerOrderByOrderedAtDesc(AccountEntity buyer);

    List<ProductOrderEntity> findByBuyerAndProductOrderStatusOrderByOrderedAtDesc(
            AccountEntity buyer, ProductOrderStatus status);

    List<ProductOrderEntity> findBySellerOrderByOrderedAtDesc(ShopEntity seller);

    List<ProductOrderEntity> findBySellerAndProductOrderStatusOrderByOrderedAtDesc(
            ShopEntity seller, ProductOrderStatus status);

    List<ProductOrderEntity> findByProductOrderStatusAndShippedAtBefore(
            ProductOrderStatus status, LocalDateTime before);

    List<ProductOrderEntity> findByProductOrderStatusAndIsDeliveryConfirmedFalse(ProductOrderStatus status);

    long countByBuyerAndProductOrderStatus(AccountEntity buyer, ProductOrderStatus status);

    long countBySellerAndProductOrderStatus(ShopEntity seller, ProductOrderStatus status);

    Optional<ProductOrderEntity> findByItems_InstallmentAgreementId(UUID installmentAgreementId);

    // ADD these paginated methods to OrderRepository.java

    Page<ProductOrderEntity> findByBuyerOrderByOrderedAtDesc(AccountEntity buyer, Pageable pageable);

    Page<ProductOrderEntity> findByBuyerAndProductOrderStatusOrderByOrderedAtDesc(
            AccountEntity buyer, ProductOrderStatus status, Pageable pageable);

    Page<ProductOrderEntity> findBySellerOrderByOrderedAtDesc(ShopEntity seller, Pageable pageable);

    Page<ProductOrderEntity> findBySellerAndProductOrderStatusOrderByOrderedAtDesc(
            ShopEntity seller, ProductOrderStatus status, Pageable pageable);
}