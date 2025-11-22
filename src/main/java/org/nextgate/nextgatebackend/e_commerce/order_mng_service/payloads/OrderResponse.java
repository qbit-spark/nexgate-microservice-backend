package org.nextgate.nextgatebackend.e_commerce.order_mng_service.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.enums.DeliveryStatus;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.enums.OrderSource;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {

    // IDs
    private UUID orderId;
    private String orderNumber;

    // Parties
    private BuyerInfo buyer;
    private SellerInfo seller;

    // Status
    private OrderStatus orderStatus;
    private DeliveryStatus deliveryStatus;
    private OrderSource orderSource;

    // Items
    private List<OrderItemResponse> items;

    // Financial
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal tax;
    private BigDecimal totalAmount;
    private BigDecimal platformFee;
    private BigDecimal sellerAmount;
    private String currency;

    // Payment
    private String paymentMethod;
    private BigDecimal amountPaid;
    private BigDecimal amountRemaining;

    // Delivery
    private String deliveryAddress;
    private String trackingNumber;
    private String carrier;

    // Confirmation
    private Boolean isDeliveryConfirmed;
    private LocalDateTime deliveryConfirmedAt;

    // Timestamps
    private LocalDateTime orderedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

    // Cancellation
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    // Nested classes
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BuyerInfo {
        private UUID accountId;
        private String userName;
        private String email;
        private String firstName;
        private String lastName;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SellerInfo {
        private UUID shopId;
        private String shopName;
        private String shopLogo;
        private String shopSlug;
    }
}