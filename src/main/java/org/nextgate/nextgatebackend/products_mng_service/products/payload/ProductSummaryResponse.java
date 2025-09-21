// ProductSummaryResponse.java - Ultra lightweight for cards
package org.nextgate.nextgatebackend.products_mng_service.products.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductCondition;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryResponse {

    // ESSENTIAL CARD INFO ONLY
    private UUID productId;
    private String productName;
    private String productSlug;
    private String shortDescription;
    private String primaryImage; // Just first image for card

    // PRICING - Essential for cards
    private BigDecimal price;
    private BigDecimal comparePrice;
    private BigDecimal finalPrice; // Price after color adjustments
    private Boolean isOnSale;
    private BigDecimal discountPercentage;

    // INVENTORY - Key for availability
    private Integer stockQuantity;
    private Boolean isInStock;
    private Boolean isLowStock;

    // BASIC DETAILS
    private String brand;
    private String sku;
    private ProductCondition condition;
    private ProductStatus status;

    // FEATURES - Important for filtering
    private Boolean isFeatured;
    private Boolean isDigital;

    // SPECIAL OFFERS - For badges/labels
    private Boolean hasGroupBuying;
    private Boolean hasInstallments;
    private Boolean hasMultipleColors;
    private BigDecimal groupPrice; // For "Group Buy: $X" badge
    private BigDecimal startingFromPrice; // Lowest price with color variations

    // TIMESTAMPS
    private LocalDateTime createdAt;


    // ShopProductsListResponse.java - Main list response
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShopProductsListResponse {
        private ShopSummaryForProducts shop;
        private ProductListSummary summary;
        private List<ProductSummaryResponse> products;
        private Integer totalProducts;
    }

    // ShopSummaryForProducts.java - Minimal shop info
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShopSummaryForProducts {
        private UUID shopId;
        private String shopName;
        private String shopSlug;
        private String logoUrl;
        private Boolean isVerified;
        private Boolean isApproved;
        private Boolean isMyShop;
    }

    // ProductListSummary.java - Quick stats
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductListSummary {
        private Integer totalProducts;
        private Integer activeProducts;
        private Integer draftProducts;
        private Integer outOfStockProducts;
        private Integer featuredProducts;
        private Integer lowStockProducts;
        private BigDecimal averagePrice;
        private BigDecimal totalInventoryValue;

        // Quick action counts
        private Integer productsWithGroupBuying;
        private Integer productsWithInstallments;
        private Integer productsWithMultipleColors;
    }
}