package org.nextgate.nextgatebackend.wishlist_service.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistResponse {
    private UserSummary user;
    private WishlistSummary wishlistSummary;
    private List<WishlistItemResponse> wishlistItems;
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
    public static class WishlistSummary {
        private Integer totalItems;
        private BigDecimal totalValue;
        private Integer inStockItems;
        private Integer outOfStockItems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WishlistItemResponse {
        private UUID wishlistId;
        private UUID productId;
        private String productName;
        private String productSlug;
        private String productImage;
        private BigDecimal unitPrice;
        private Boolean isOnSale;
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
