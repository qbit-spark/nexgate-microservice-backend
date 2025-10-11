package org.nextgate.nextgatebackend.order_mng_service.utils;

import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.order_mng_service.entity.OrderEntity;
import org.nextgate.nextgatebackend.order_mng_service.entity.OrderItemEntity;
import org.nextgate.nextgatebackend.order_mng_service.payloads.OrderItemResponse;
import org.nextgate.nextgatebackend.order_mng_service.payloads.OrderResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    // ========================================
    // SINGLE ORDER MAPPING
    // ========================================

    public GlobeSuccessResponseBuilder toOrderResponse(OrderEntity order) {

        if (order == null) {
            return GlobeSuccessResponseBuilder.builder()
                    .message("Order not found")
                    .data(null)
                    .build();
        }

        OrderResponse orderResponse = mapToOrderResponse(order);

        return GlobeSuccessResponseBuilder.builder()
                .message("Order retrieved successfully")
                .data(orderResponse)
                .build();
    }


    // ========================================
    // LIST OF ORDERS MAPPING
    // ========================================

    public GlobeSuccessResponseBuilder toOrderResponseList(List<OrderEntity> orders) {

        List<OrderResponse> orderResponses = orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());

        return GlobeSuccessResponseBuilder.builder()
                .message(orderResponses.isEmpty()
                        ? "No orders found"
                        : "Orders retrieved successfully")
                .data(orderResponses)
                .build();
    }


    // ========================================
    // HELPER: MAP SINGLE ORDER (WITHOUT WRAPPING)
    // ========================================

    private OrderResponse mapToOrderResponse(OrderEntity order) {
        return OrderResponse.builder()
                // IDs
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())

                // Parties
                .buyer(mapBuyerInfo(order))
                .seller(mapSellerInfo(order))

                // Status
                .orderStatus(order.getOrderStatus())
                .deliveryStatus(order.getDeliveryStatus())
                .orderSource(order.getOrderSource())

                // Items
                .items(mapOrderItems(order.getItems()))

                // Financial
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .tax(order.getTax())
                .discount(order.getDiscount())
                .totalAmount(order.getTotalAmount())
                .platformFee(order.getPlatformFee())
                .sellerAmount(order.getSellerAmount())
                .currency(order.getCurrency())

                // Payment
                .paymentMethod(order.getPaymentMethod())
                .amountPaid(order.getAmountPaid())
                .amountRemaining(order.getAmountRemaining())

                // Delivery
                .deliveryAddress(order.getDeliveryAddress())
                .trackingNumber(order.getTrackingNumber())
                .carrier(order.getCarrier())

                // Confirmation
                .isDeliveryConfirmed(order.getIsDeliveryConfirmed())
                .deliveryConfirmedAt(order.getDeliveryConfirmedAt())

                // Timestamps
                .orderedAt(order.getOrderedAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())

                // Cancellation
                .cancelledAt(order.getCancelledAt())
                .cancellationReason(order.getCancellationReason())

                .build();
    }


    // ========================================
    // HELPER: MAP BUYER INFO
    // ========================================

    private OrderResponse.BuyerInfo mapBuyerInfo(OrderEntity order) {
        if (order.getBuyer() == null) {
            return null;
        }

        return OrderResponse.BuyerInfo.builder()
                .accountId(order.getBuyer().getAccountId())
                .userName(order.getBuyer().getUserName())
                .email(order.getBuyer().getEmail())
                .firstName(order.getBuyer().getFirstName())
                .lastName(order.getBuyer().getLastName())
                .build();
    }


    // ========================================
    // HELPER: MAP SELLER INFO
    // ========================================

    private OrderResponse.SellerInfo mapSellerInfo(OrderEntity order) {
        if (order.getSeller() == null) {
            return null;
        }

        return OrderResponse.SellerInfo.builder()
                .shopId(order.getSeller().getShopId())
                .shopName(order.getSeller().getShopName())
                .shopLogo(order.getSeller().getLogoUrl())
                .shopSlug(order.getSeller().getShopSlug())
                .build();
    }


    // ========================================
    // HELPER: MAP ORDER ITEMS
    // ========================================

    private List<OrderItemResponse> mapOrderItems(List<OrderItemEntity> items) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
                .map(this::mapOrderItem)
                .collect(Collectors.toList());
    }


    // ========================================
    // HELPER: MAP SINGLE ORDER ITEM
    // ========================================

    private OrderItemResponse mapOrderItem(OrderItemEntity item) {
        return OrderItemResponse.builder()
                .orderItemId(item.getOrderItemId())
                .productId(item.getProduct().getProductId())
                .productName(item.getProductName())
                .productSlug(item.getProductSlug())
                .productImage(item.getProductImage())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .tax(item.getTax())
                .total(item.getTotal())
                .build();
    }
}