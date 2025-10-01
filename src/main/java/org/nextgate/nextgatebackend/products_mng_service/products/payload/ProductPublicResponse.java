package org.nextgate.nextgatebackend.products_mng_service.products.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductCondition;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPublicResponse {

    // Basic Information
    private UUID productId;
    private String productName;
    private String productSlug;
    private String productDescription;
    private String shortDescription;
    private List<String> productImages;

    // Pricing Information
    private BigDecimal price;
    private BigDecimal comparePrice;
    private BigDecimal discountAmount;
    private BigDecimal discountPercentage;
    private Boolean isOnSale;

    // Availability Information
    private Boolean isInStock;
    private Boolean isLowStock;
    private Integer stockQuantity; // Only show if > 0

    // Product Details
    private String brand;
    private ProductCondition condition;

    // SEO and Tags
    private List<String> tags;

    // Shop Information (minimal)
    private UUID shopId;
    private String shopName;
    private String shopSlug;
    private String shopLogoUrl;

    // Category Information
    private UUID categoryId;
    private String categoryName;

    // Features
    private Boolean isDigital;
    private Boolean requiresShipping;

    // Product Features - Specifications
    private Map<String, String> specifications;
    private Boolean hasSpecifications;

    // Product Features - Colors
    private List<ProductColorPublicResponse> colors;
    private Boolean hasMultipleColors;
    private PriceRangePublicResponse priceRange;

    // Special Offers
    private GroupBuyingPublicResponse groupBuying;
    private InstallmentPublicResponse installmentOptions;

    // Timestamp
    private LocalDateTime createdAt;

    // Nested classes for public responses
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductColorPublicResponse {
        private String name;
        private String hex;
        private List<String> images;
        private BigDecimal priceAdjustment;
        private BigDecimal finalPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceRangePublicResponse {
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private Boolean hasPriceVariations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupBuyingPublicResponse {
        private Boolean isAvailable;
        private Integer maxGroupSize;
        private BigDecimal groupPrice;
        private BigDecimal groupDiscount;
        private BigDecimal groupDiscountPercentage;
        private Integer timeLimitHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstallmentPublicResponse {
        private Boolean isAvailable;
        private List<InstallmentPlanPublicResponse> plans;
        private Boolean downPaymentRequired;
        private BigDecimal minDownPaymentPercentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstallmentPlanPublicResponse {
        private Integer duration;
        private String interval;
        private BigDecimal interestRate;
        private String description;
    }
}