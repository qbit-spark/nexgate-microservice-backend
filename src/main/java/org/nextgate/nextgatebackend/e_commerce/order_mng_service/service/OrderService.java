package org.nextgate.nextgatebackend.e_commerce.order_mng_service.service;

import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.entity.OrderEntity;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.enums.OrderStatus;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    // ========================================
    // ORDER CREATION
    // ========================================

    /**
     * Creates order from a completed checkout session.
     * Handles all session types automatically.
     */
    List<UUID> createOrdersFromCheckoutSession(UUID checkoutSessionId)
            throws ItemNotFoundException, BadRequestException;

    // ========================================
    // QUERY METHODS - SINGLE ORDER
    // ========================================

    OrderEntity getOrderById(UUID orderId, AccountEntity requester)
            throws ItemNotFoundException, BadRequestException;


    OrderEntity getOrderByNumber(String orderNumber, AccountEntity requester)
            throws ItemNotFoundException, BadRequestException;

    // ========================================
    // QUERY METHODS - CUSTOMER ORDERS
    // ========================================


    List<OrderEntity> getMyOrders(AccountEntity customer);

    List<OrderEntity> getMyOrdersByStatus(AccountEntity customer, OrderStatus status);

    // ========================================
    // QUERY METHODS - SHOP ORDERS
    // ========================================

    List<OrderEntity> getShopOrders(ShopEntity shop);


    List<OrderEntity> getShopOrdersByStatus(ShopEntity shop, OrderStatus status);

    // ========================================
    // ORDER STATUS UPDATES - SELLER ACTIONS
    // ========================================

    void markOrderAsShipped(UUID orderId, AccountEntity seller) throws ItemNotFoundException, BadRequestException;

    Page<OrderEntity> getMyOrdersPaged(AccountEntity customer, int page, int size);

    Page<OrderEntity> getMyOrdersByStatusPaged(AccountEntity customer, OrderStatus status, int page, int size);


    Page<OrderEntity> getShopOrdersPaged(ShopEntity shop, int page, int size);


    Page<OrderEntity> getShopOrdersByStatusPaged(ShopEntity shop, OrderStatus status, int page, int size);


    void confirmDelivery(
            UUID orderId,
            String confirmationCode,
            AccountEntity customer,
            String ipAddress,
            String deviceInfo
    ) throws ItemNotFoundException, BadRequestException, RandomExceptions;


    void regenerateDeliveryConfirmationCode(UUID orderId, AccountEntity customer) throws ItemNotFoundException, BadRequestException;
}