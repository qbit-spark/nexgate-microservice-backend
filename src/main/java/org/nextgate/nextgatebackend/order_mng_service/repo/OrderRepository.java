package org.nextgate.nextgatebackend.order_mng_service.repo;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.order_mng_service.entity.OrderEntity;
import org.nextgate.nextgatebackend.order_mng_service.enums.OrderStatus;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    Optional<OrderEntity> findByCheckoutSessionId(UUID checkoutSessionId);

    List<OrderEntity> findByBuyerOrderByOrderedAtDesc(AccountEntity buyer);

    List<OrderEntity> findByBuyerAndOrderStatusOrderByOrderedAtDesc(
            AccountEntity buyer, OrderStatus status);

    List<OrderEntity> findBySellerOrderByOrderedAtDesc(ShopEntity seller);

    List<OrderEntity> findBySellerAndOrderStatusOrderByOrderedAtDesc(
            ShopEntity seller, OrderStatus status);

    List<OrderEntity> findByOrderStatusAndShippedAtBefore(
            OrderStatus status, LocalDateTime before);

    List<OrderEntity> findByOrderStatusAndIsDeliveryConfirmedFalse(OrderStatus status);

    long countByBuyerAndOrderStatus(AccountEntity buyer, OrderStatus status);

    long countBySellerAndOrderStatus(ShopEntity seller, OrderStatus status);

    Optional<OrderEntity> findByItems_InstallmentAgreementId(UUID installmentAgreementId);
}