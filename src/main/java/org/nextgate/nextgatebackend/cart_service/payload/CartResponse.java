package org.nextgate.nextgatebackend.cart_service.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// MAIN CART RESPONSE
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private UserSummary user;
    private CartSummary cartSummary;
    private List<CartItemResponse> cartItems;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private UUID userId;
        private String userName;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartSummary {
        private Integer totalItems;
        private Integer totalQuantity;
        private BigDecimal subtotal;
        private BigDecimal totalDiscount;
        private BigDecimal totalAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemResponse {
        private UUID itemId;
        private UUID productId;
        private String productName;
        private String productSlug;
        private String productImage;
        private BigDecimal unitPrice;
       // private BigDecimal discountAmount;
        private Integer quantity;
        private BigDecimal itemSubtotal;
        private BigDecimal itemDiscount;
        private BigDecimal totalPrice;
        private ShopSummary shop;
        private ProductAvailability availability;
        private LocalDateTime addedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShopSummary {
        private UUID shopId;
        private String shopName;
        private String shopSlug;
        private String logoUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductAvailability {
        private Boolean inStock;
        private Integer stockQuantity;
    }
}