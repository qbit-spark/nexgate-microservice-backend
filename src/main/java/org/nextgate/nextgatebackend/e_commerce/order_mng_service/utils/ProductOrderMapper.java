package org.nextgate.nextgatebackend.e_commerce.order_mng_service.utils;

import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.entity.ProductOrderEntity;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.entity.ProductOrderItemEntity;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.payloads.ProductOrderItemResponse;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.payloads.ProductOrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductOrderMapper {

    // ========================================
    // SINGLE ORDER MAPPING
    // ========================================

    public GlobeSuccessResponseBuilder toOrderResponse(ProductOrderEntity order) {

        if (order == null) {
            return GlobeSuccessResponseBuilder.builder()
                    .message("Order not found")
                    .data(null)
                    .build();
        }

        ProductOrderResponse productOrderResponse = mapToOrderResponse(order);

        return GlobeSuccessResponseBuilder.builder()
                .message("Order retrieved successfully")
                .data(productOrderResponse)
                .build();
    }


    // ========================================
    // LIST OF ORDERS MAPPING
    // ========================================

    public GlobeSuccessResponseBuilder toOrderResponseList(List<ProductOrderEntity> orders) {

        List<ProductOrderResponse> productOrderRespons = orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());

        return GlobeSuccessResponseBuilder.builder()
                .message(productOrderRespons.isEmpty()
                        ? "No orders found"
                        : "Orders retrieved successfully")
                .data(productOrderRespons)
                .build();
    }


    // ========================================
    // HELPER: MAP SINGLE ORDER (WITHOUT WRAPPING)
    // ========================================

    private ProductOrderResponse mapToOrderResponse(ProductOrderEntity order) {
        return ProductOrderResponse.builder()
                // IDs
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())

                // Parties
                .buyer(mapBuyerInfo(order))
                .seller(mapSellerInfo(order))

                // Status
                .productOrderStatus(order.getProductOrderStatus())
                .deliveryStatus(order.getDeliveryStatus())
                .productOrderSource(order.getProductOrderSource())

                // Items
                .items(mapOrderItems(order.getItems()))

                // Financial
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .tax(order.getTax())
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

    private ProductOrderResponse.BuyerInfo mapBuyerInfo(ProductOrderEntity order) {
        if (order.getBuyer() == null) {
            return null;
        }

        return ProductOrderResponse.BuyerInfo.builder()
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

    private ProductOrderResponse.SellerInfo mapSellerInfo(ProductOrderEntity order) {
        if (order.getSeller() == null) {
            return null;
        }

        return ProductOrderResponse.SellerInfo.builder()
                .shopId(order.getSeller().getShopId())
                .shopName(order.getSeller().getShopName())
                .shopLogo(order.getSeller().getLogoUrl())
                .shopSlug(order.getSeller().getShopSlug())
                .build();
    }


    // ========================================
    // HELPER: MAP ORDER ITEMS
    // ========================================

    private List<ProductOrderItemResponse> mapOrderItems(List<ProductOrderItemEntity> items) {
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

    private ProductOrderItemResponse mapOrderItem(ProductOrderItemEntity item) {
        return ProductOrderItemResponse.builder()
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


    public GlobeSuccessResponseBuilder toOrderPageResponse(Page<ProductOrderEntity> orderPage) {

        List<ProductOrderResponse> productOrderRespons = orderPage.getContent().stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());

        var responseData = new Object() {
            public final List<ProductOrderResponse> orders = productOrderRespons;
            public final int currentPage = orderPage.getNumber() + 1; // Convert back to 1-based
            public final int pageSize = orderPage.getSize();
            public final long totalElements = orderPage.getTotalElements();
            public final int totalPages = orderPage.getTotalPages();
            public final boolean hasNext = orderPage.hasNext();
            public final boolean hasPrevious = orderPage.hasPrevious();
            public final boolean isFirst = orderPage.isFirst();
            public final boolean isLast = orderPage.isLast();
        };

        return GlobeSuccessResponseBuilder.builder()
                .message(productOrderRespons.isEmpty()
                        ? "No orders found"
                        : "Orders retrieved successfully")
                .data(responseData)
                .build();
    }
}