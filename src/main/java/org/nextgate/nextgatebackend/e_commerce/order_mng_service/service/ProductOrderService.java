package org.nextgate.nextgatebackend.e_commerce.order_mng_service.service;

import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.entity.ProductOrderEntity;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.enums.ProductOrderStatus;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface ProductOrderService {

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

    ProductOrderEntity getOrderById(UUID orderId, AccountEntity requester)
            throws ItemNotFoundException, BadRequestException;


    ProductOrderEntity getOrderByNumber(String orderNumber, AccountEntity requester)
            throws ItemNotFoundException, BadRequestException;

    // ========================================
    // QUERY METHODS - CUSTOMER ORDERS
    // ========================================


    List<ProductOrderEntity> getMyOrders(AccountEntity customer);

    List<ProductOrderEntity> getMyOrdersByStatus(AccountEntity customer, ProductOrderStatus status);

    // ========================================
    // QUERY METHODS - SHOP ORDERS
    // ========================================

    List<ProductOrderEntity> getShopOrders(ShopEntity shop);


    List<ProductOrderEntity> getShopOrdersByStatus(ShopEntity shop, ProductOrderStatus status);

    // ========================================
    // ORDER STATUS UPDATES - SELLER ACTIONS
    // ========================================

    void markOrderAsShipped(UUID orderId, AccountEntity seller) throws ItemNotFoundException, BadRequestException;

    Page<ProductOrderEntity> getMyOrdersPaged(AccountEntity customer, int page, int size);

    Page<ProductOrderEntity> getMyOrdersByStatusPaged(AccountEntity customer, ProductOrderStatus status, int page, int size);


    Page<ProductOrderEntity> getShopOrdersPaged(ShopEntity shop, int page, int size);


    Page<ProductOrderEntity> getShopOrdersByStatusPaged(ShopEntity shop, ProductOrderStatus status, int page, int size);


    void confirmDelivery(
            UUID orderId,
            String confirmationCode,
            AccountEntity customer,
            String ipAddress,
            String deviceInfo
    ) throws ItemNotFoundException, BadRequestException, RandomExceptions;


    void regenerateDeliveryConfirmationCode(UUID orderId, AccountEntity customer) throws ItemNotFoundException, BadRequestException;
}