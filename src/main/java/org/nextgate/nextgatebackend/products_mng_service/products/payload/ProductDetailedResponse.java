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
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailedResponse {

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

    // Inventory Information
    private Integer stockQuantity;
    private Integer lowStockThreshold;
    private Boolean isInStock;
    private Boolean isLowStock;
    private Boolean trackInventory;

    // Product Details
    private String brand;
    private String sku;
    private ProductCondition condition;
    private ProductStatus status;

    // SEO and Tags
    private List<String> tags;
    private String metaTitle;
    private String metaDescription;

    // Shop Information
    private UUID shopId;
    private String shopName;
    private String shopSlug;

    // Category Information
    private UUID categoryId;
    private String categoryName;

    // Features
    private Boolean isFeatured;
    private Boolean isDigital;
    private Boolean requiresShipping;

    // System Fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID editedBy;

    // NEW FEATURES - Specifications
    private Map<String, String> specifications;
    private Boolean hasSpecifications;
    private Integer specificationCount;

    // NEW FEATURES - Colors
    private List<ColorDetailedResponse> colors;
    private Boolean hasMultipleColors;
    private Integer colorCount;
    private PriceRangeResponse priceRange;

    // NEW FEATURES - Ordering Limits
    private OrderingLimitsResponse orderingLimits;

    // NEW FEATURES - Group Buying
    private OrderingLimitsResponse.GroupBuyingDetailedResponse groupBuying;

    // NEW FEATURES - Installment Options
    private OrderingLimitsResponse.InstallmentOptionsDetailedResponse installmentOptions;

    // Purchase Options Summary
    private OrderingLimitsResponse.PurchaseOptionsResponse purchaseOptions;

    // Static Nested Classes
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColorDetailedResponse {
        private String name;
        private String hex;
        private List<String> images;
        private BigDecimal priceAdjustment;
        private BigDecimal finalPrice;
        private Boolean hasExtraFee;
        private String extraFeeReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceRangeResponse {
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private BigDecimal priceStartsFrom;
        private Boolean hasPriceVariations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderingLimitsResponse {
        private Integer minOrderQuantity;
        private Integer maxOrderQuantity;
        private Boolean requiresApproval;
        private Integer canOrderQuantity;
        private Integer maxAllowedQuantity;
        private Boolean hasOrderingLimits;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class GroupBuyingDetailedResponse {
            private Boolean isEnabled;
            private Boolean isAvailable;
            private Integer minGroupSize;
            private Integer maxGroupSize;
            private Integer currentGroupSize;
            private Integer remainingSlots;
            private Double progressPercentage;
            private BigDecimal groupPrice;
            private BigDecimal groupDiscount;
            private BigDecimal groupDiscountPercentage;
            private Integer timeLimitHours;
            private Long timeRemainingHours;
            private LocalDateTime expiresAt;
            private Boolean requiresMinimum;
            private String status;
            private Boolean canJoinGroup;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class InstallmentOptionsDetailedResponse {
            private Boolean isEnabled;
            private Boolean isAvailable;
            private Boolean downPaymentRequired;
            private BigDecimal minDownPaymentPercentage;
            private List<InstallmentPlanDetailedResponse> plans;
            private String eligibilityStatus;
            private Boolean creditCheckRequired;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class InstallmentPlanDetailedResponse {
            private String planId;
            private Integer duration;
            private String interval;
            private BigDecimal interestRate;
            private String description;
            private InstallmentCalculationResponse calculations;
            private List<PaymentScheduleResponse> paymentSchedule;
            private Boolean isPopular;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class InstallmentCalculationResponse {
            private BigDecimal downPayment;
            private BigDecimal remainingAmount;
            private BigDecimal totalInterest;
            private BigDecimal paymentAmount;
            private BigDecimal totalAmount;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PaymentScheduleResponse {
            private Integer paymentNumber;
            private BigDecimal amount;
            private LocalDateTime dueDate;
            private String description;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PurchaseOptionsResponse {
            private Boolean canBuyNow;
            private Boolean canJoinGroup;
            private Boolean canPayInstallment;
            private String recommendedOption;
            private BestDealResponse bestDeal;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class BestDealResponse {
            private String option;
            private BigDecimal savings;
            private BigDecimal finalPrice;
        }
    }
}