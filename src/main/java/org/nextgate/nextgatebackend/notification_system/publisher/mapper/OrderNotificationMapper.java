package org.nextgate.nextgatebackend.notification_system.publisher.mapper;

import org.nextgate.nextgatebackend.order_mng_service.entity.OrderEntity;
import org.nextgate.nextgatebackend.order_mng_service.entity.OrderItemEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mapper for Order notification data
 * Handles BOTH buyer and seller perspectives
 */
public class OrderNotificationMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== ORDER CONFIRMATION (BUYER PERSPECTIVE) ====================

    /**
     * Map order confirmation for BUYER
     *
     * Template variables:
     * - {{customer.name}}
     * - {{customer.email}}
     * - {{orderId}}
     * - {{orderNumber}}
     * - {{orderDate}}
     * - {{items}} (list)
     * - {{payment.amount}}
     * - {{payment.method}}
     * - {{shipping.address}}
     * - {{shop.name}}
     * - {{shop.logo}}
     */
    public static Map<String, Object> mapOrderConfirmationForBuyer(OrderEntity order) {
        Map<String, Object> data = new HashMap<>();

        // Customer information
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", order.getBuyer().getFirstName());
        customer.put("email", order.getBuyer().getEmail());
        data.put("customer", customer);

        // Order basic info
        data.put("orderId", order.getOrderId().toString());
        data.put("orderNumber", order.getOrderNumber());
        data.put("orderDate", order.getOrderedAt().format(DATE_FORMATTER));

        // Items list
        List<Map<String, Object>> items = order.getItems().stream()
                .map(OrderNotificationMapper::mapOrderItem)
                .collect(Collectors.toList());
        data.put("items", items);

        // Payment info
        Map<String, Object> payment = new HashMap<>();
        payment.put("amount", order.getTotalAmount().toString());
        payment.put("method", order.getPaymentMethod());
        payment.put("currency", order.getCurrency());
        data.put("payment", payment);

        // Shipping info
        Map<String, Object> shipping = new HashMap<>();
        shipping.put("address", order.getDeliveryAddress());
        shipping.put("fee", order.getShippingFee().toString());
        data.put("shipping", shipping);

        // Shop info (where they bought from)
        Map<String, Object> shop = new HashMap<>();
        shop.put("name", order.getSeller().getShopName());
        shop.put("logo", order.getSeller().getLogoUrl());
        data.put("shop", shop);

        // Timestamp
        data.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

        return data;
    }

    // ==================== NEW ORDER (SELLER PERSPECTIVE) ====================

    /**
     * Map new order notification for SELLER
     *
     * Template variables:
     * - {{shop.name}}
     * - {{orderId}}
     * - {{orderNumber}}
     * - {{orderDate}}
     * - {{customer.name}}
     * - {{customer.email}}
     * - {{items}} (list)
     * - {{order.totalAmount}}
     * - {{order.itemCount}}
     * - {{seller.amount}} (what seller gets after platform fee)
     * - {{platform.fee}}
     * - {{shipping.address}}
     */
    public static Map<String, Object> mapNewOrderForSeller(OrderEntity order) {
        Map<String, Object> data = new HashMap<>();

        // Shop information (recipient)
        Map<String, Object> shop = new HashMap<>();
        shop.put("name", order.getSeller().getShopName());
        shop.put("id", order.getSeller().getShopId().toString());
        data.put("shop", shop);

        // Order basic info
        data.put("orderId", order.getOrderId().toString());
        data.put("orderNumber", order.getOrderNumber());
        data.put("orderDate", order.getOrderedAt().format(DATE_FORMATTER));

        // Customer info (who placed the order)
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", order.getBuyer().getFirstName() + " " + order.getBuyer().getLastName());
        customer.put("email", order.getBuyer().getEmail());
        customer.put("phone", order.getBuyer().getPhoneNumber());
        data.put("customer", customer);

        // Items list
        List<Map<String, Object>> items = order.getItems().stream()
                .map(OrderNotificationMapper::mapOrderItem)
                .collect(Collectors.toList());
        data.put("items", items);

        // Order financials
        Map<String, Object> orderInfo = new HashMap<>();
        orderInfo.put("totalAmount", order.getTotalAmount().toString());
        orderInfo.put("itemCount", order.getTotalItemCount());
        orderInfo.put("currency", order.getCurrency());
        data.put("order", orderInfo);

        // Seller earnings (important for sellers!)
        Map<String, Object> seller = new HashMap<>();
        seller.put("amount", order.getSellerAmount().toString());
        data.put("seller", seller);

        // Platform fee
        Map<String, Object> platform = new HashMap<>();
        platform.put("fee", order.getPlatformFee().toString());
        data.put("platform", platform);

        // Shipping information (where to send)
        Map<String, Object> shipping = new HashMap<>();
        shipping.put("address", order.getDeliveryAddress());
        data.put("shipping", shipping);

        // Timestamp
        data.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

        return data;
    }

    // ==================== ORDER SHIPPED (BUYER PERSPECTIVE) ====================

    /**
     * Map order shipped notification for BUYER
     */
    public static Map<String, Object> mapOrderShippedForBuyer(
            OrderEntity order,
            String confirmationCode) {

        Map<String, Object> data = new HashMap<>();

        // Customer info
        data.put("customer", Map.of("name", order.getBuyer().getFirstName()));

        // Order info
        data.put("orderId", order.getOrderId().toString());
        data.put("orderNumber", order.getOrderNumber());
        data.put("shippedAt", order.getShippedAt().format(DATE_FORMATTER));

        // Tracking info
        Map<String, Object> tracking = new HashMap<>();
        tracking.put("number", order.getTrackingNumber());
        tracking.put("carrier", order.getCarrier());
        data.put("tracking", tracking);

        // Confirmation code (for delivery confirmation)
        data.put("confirmationCode", confirmationCode);

        // Items summary
        data.put("itemCount", order.getTotalItemCount());
        data.put("items", order.getItems().stream()
                .map(OrderNotificationMapper::mapOrderItemSimple)
                .collect(Collectors.toList()));

        // Shop info
        data.put("shop", Map.of("name", order.getSeller().getShopName()));

        return data;
    }

    // ==================== ORDER DELIVERED (BUYER PERSPECTIVE) ====================

    /**
     * Map order delivered notification for BUYER
     */
    public static Map<String, Object> mapOrderDeliveredForBuyer(OrderEntity order) {
        Map<String, Object> data = new HashMap<>();

        // Customer info
        data.put("customer", Map.of(
                "name", order.getBuyer().getFirstName(),
                "email", order.getBuyer().getEmail()
        ));

        // Order info
        data.put("orderId", order.getOrderId().toString());
        data.put("orderNumber", order.getOrderNumber());
        data.put("deliveredAt", order.getDeliveredAt().format(DATE_FORMATTER));

        // Items
        data.put("itemCount", order.getTotalItemCount());
        data.put("items", order.getItems().stream()
                .map(OrderNotificationMapper::mapOrderItemSimple)
                .collect(Collectors.toList()));

        // Shop info
        data.put("shop", Map.of("name", order.getSeller().getShopName()));

        // Escrow info (money released)
        Map<String, Object> escrow = new HashMap<>();
        escrow.put("released", order.getIsEscrowReleased());
        escrow.put("amount", order.getSellerAmount().toString());
        data.put("escrow", escrow);

        data.put("recipientRole", "BUYER");

        return data;
    }

    // ==================== ORDER DELIVERED (SELLER PERSPECTIVE) ====================

    /**
     * Map order delivered notification for SELLER
     * Tells seller that customer confirmed delivery
     */
    public static Map<String, Object> mapOrderDeliveredForSeller(OrderEntity order) {
        Map<String, Object> data = new HashMap<>();

        // Shop info
        data.put("shop", Map.of("name", order.getSeller().getShopName()));

        // Order info
        data.put("orderId", order.getOrderId().toString());
        data.put("orderNumber", order.getOrderNumber());
        data.put("deliveredAt", order.getDeliveredAt().format(DATE_FORMATTER));

        // Customer info
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", order.getBuyer().getFirstName() + " " + order.getBuyer().getLastName());
        data.put("customer", customer);

        // Items
        data.put("itemCount", order.getTotalItemCount());
        data.put("items", order.getItems().stream()
                .map(OrderNotificationMapper::mapOrderItemSimple)
                .collect(Collectors.toList()));

        // Earnings info
        Map<String, Object> seller = new HashMap<>();
        seller.put("amount", order.getSellerAmount().toString());
        data.put("seller", seller);

        // Currency
        data.put("currency", order.getCurrency());

        data.put("recipientRole", "SELLER");

        return data;
    }

    // ==================== ESCROW RELEASED / PAYMENT TO SELLER ====================

    /**
     * Map escrow released / payment notification for SELLER
     * This is like a wallet top-up notification but for seller earnings
     */
    public static Map<String, Object> mapEscrowReleasedForSeller(
            OrderEntity order,
            BigDecimal previousBalance,
            BigDecimal newBalance) {

        Map<String, Object> data = new HashMap<>();

        // Shop info (recipient)
        data.put("shop", Map.of("name", order.getSeller().getShopName()));

        // Transaction info (like wallet top-up)
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("type", "CREDIT");
        transaction.put("amount", order.getSellerAmount().toString());
        transaction.put("id", order.getOrderNumber()); // Use order number as reference
        transaction.put("source", "ORDER_PAYMENT");
        data.put("transaction", transaction);

        // Wallet/Balance info
        Map<String, Object> wallet = new HashMap<>();
        wallet.put("previousBalance", previousBalance.toString());
        wallet.put("currentBalance", newBalance.toString());
        wallet.put("change", order.getSellerAmount().toString());
        data.put("wallet", wallet);

        // Order reference
        Map<String, Object> orderInfo = new HashMap<>();
        orderInfo.put("number", order.getOrderNumber());
        orderInfo.put("itemCount", order.getTotalItemCount());
        data.put("order", orderInfo);

        // Platform fee breakdown
        Map<String, Object> breakdown = new HashMap<>();
        breakdown.put("orderTotal", order.getTotalAmount().toString());
        breakdown.put("platformFee", order.getPlatformFee().toString());
        breakdown.put("yourEarnings", order.getSellerAmount().toString());
        data.put("breakdown", breakdown);

        // Currency
        data.put("currency", order.getCurrency());

        // Timestamp
        data.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

        return data;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Map full order item details
     */
    private static Map<String, Object> mapOrderItem(OrderItemEntity item) {
        Map<String, Object> itemMap = new HashMap<>();
        itemMap.put("name", item.getProductName());
        itemMap.put("sku", item.getProductSlug());
        itemMap.put("quantity", item.getQuantity());
        itemMap.put("price", item.getUnitPrice().toString());
        itemMap.put("total", item.getTotal().toString());
        itemMap.put("image", item.getProductImage());
        return itemMap;
    }

    /**
     * Map simplified order item (for lists)
     */
    private static Map<String, Object> mapOrderItemSimple(OrderItemEntity item) {
        Map<String, Object> itemMap = new HashMap<>();
        itemMap.put("name", item.getProductName());
        itemMap.put("quantity", item.getQuantity());
        return itemMap;
    }
}