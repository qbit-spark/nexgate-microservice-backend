package org.nextgate.nextgatebackend.order_mng_service.service;

import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.order_mng_service.entity.OrderEntity;
import org.nextgate.nextgatebackend.order_mng_service.enums.OrderStatus;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;

import java.math.BigDecimal;
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

    /**
     * Get order by ID.
     * Validates that requester has access to this order.
     */
    OrderEntity getOrderById(UUID orderId, AccountEntity requester)
            throws ItemNotFoundException, BadRequestException;

    /**
     * Get order by order number.
     * Validates that requester has access to this order.
     */
    OrderEntity getOrderByNumber(String orderNumber, AccountEntity requester)
            throws ItemNotFoundException, BadRequestException;

    // ========================================
    // QUERY METHODS - CUSTOMER ORDERS
    // ========================================

    /**
     * Get all orders for a customer.
     */
    List<OrderEntity> getMyOrders(AccountEntity customer);

    /**
     * Get customer orders filtered by status.
     */
    List<OrderEntity> getMyOrdersByStatus(AccountEntity customer, OrderStatus status);

    // ========================================
    // QUERY METHODS - SHOP ORDERS
    // ========================================

    /**
     * Get all orders for a shop (seller).
     */
    List<OrderEntity> getShopOrders(ShopEntity shop);

    /**
     * Get shop orders filtered by status.
     */
    List<OrderEntity> getShopOrdersByStatus(ShopEntity shop, OrderStatus status);

    // ========================================
    // ORDER STATUS UPDATES - SELLER ACTIONS
    // ========================================

    /**
     * Mark order as shipped by seller.
     */
    void markOrderAsShipped(
            UUID orderId,
            String trackingNumber,
            String carrier,
            AccountEntity seller
    ) throws ItemNotFoundException, BadRequestException;

    /**
     * Mark order as delivered by seller.
     */
    void markOrderAsDelivered(UUID orderId, AccountEntity seller)
            throws ItemNotFoundException, BadRequestException;

    // ========================================
    // ORDER STATUS UPDATES - CUSTOMER ACTIONS
    // ========================================

    /**
     * Customer confirms delivery.
     * This triggers escrow release to seller.
     */
    void confirmDelivery(UUID orderId, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException;

    // ========================================
    // ORDER CANCELLATION
    // ========================================

    /**
     * Cancel order.
     * Can be done by customer (before shipping) or seller.
     */
    void cancelOrder(UUID orderId, String reason, AccountEntity actor)
            throws ItemNotFoundException, BadRequestException;

    // ========================================
    // INSTALLMENT-SPECIFIC METHODS
    // ========================================

    /**
     * Update payment progress for installment orders.
     * Called when customer makes installment payment.
     */
    void updateInstallmentPaymentProgress(
            UUID orderId,
            BigDecimal amountPaid,
            BigDecimal totalAmount,
            Integer paymentsCompleted,
            Integer totalPayments
    ) throws ItemNotFoundException;

    /**
     * Mark installment order as fully paid.
     */
    void markOrderAsFullyPaid(UUID orderId) throws ItemNotFoundException;

    // ========================================
    // ESCROW MANAGEMENT
    // ========================================

    /**
     * Release escrow funds to seller after delivery confirmation.
     * CRITICAL: This is where seller gets paid!
     */
    void releaseEscrowForOrder(UUID orderId)
            throws ItemNotFoundException, BadRequestException;
}