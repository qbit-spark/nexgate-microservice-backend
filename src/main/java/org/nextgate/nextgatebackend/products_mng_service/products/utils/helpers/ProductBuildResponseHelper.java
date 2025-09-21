package org.nextgate.nextgatebackend.products_mng_service.products.utils.helpers;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductStatus;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.ProductDetailedResponse;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.ProductPublicResponse;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.ProductSummaryResponse;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductBuildResponseHelper {
    public ProductDetailedResponse buildDetailedProductResponse(ProductEntity product) {
        return ProductDetailedResponse.builder()
                // Basic Information
                .productId(product.getProductId())
                .productName(product.getProductName())
                .productSlug(product.getProductSlug())
                .productDescription(product.getProductDescription())
                .shortDescription(product.getShortDescription())
                .productImages(product.getProductImages())

                // Pricing Information
                .price(product.getPrice())
                .comparePrice(product.getComparePrice())
                .discountAmount(product.getDiscountAmount())
                .discountPercentage(product.getDiscountPercentage())
                .isOnSale(product.isOnSale())

                // Inventory Information
                .stockQuantity(product.getStockQuantity())
                .lowStockThreshold(product.getLowStockThreshold())
                .isInStock(product.isInStock())
                .isLowStock(product.isLowStock())
                .trackInventory(product.getTrackInventory())

                // Product Details
                .brand(product.getBrand())
                .sku(product.getSku())
                .condition(product.getCondition())
                .status(product.getStatus())

                // SEO and Tags
                .tags(product.getTags())
                .metaTitle(product.getMetaTitle())
                .metaDescription(product.getMetaDescription())

                // Shop Information
                .shopId(product.getShop().getShopId())
                .shopName(product.getShop().getShopName())
                .shopSlug(product.getShop().getShopSlug())

                // Category Information
                .categoryId(product.getCategory().getCategoryId())
                .categoryName(product.getCategory().getCategoryName())

                // Features
                .isFeatured(product.getIsFeatured())
                .isDigital(product.getIsDigital())
                .requiresShipping(product.getRequiresShipping())

                // System Fields
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .createdBy(product.getCreatedBy())
                .editedBy(product.getEditedBy())

                // NEW FEATURES - Specifications
                .specifications(product.getSpecifications())
                .hasSpecifications(product.hasSpecifications())
                .specificationCount(product.getSpecificationCount())

                // NEW FEATURES - Colors
                .colors(buildColorResponses(product.getColors(), product.getPrice()))
                .hasMultipleColors(product.hasMultipleColors())
                .colorCount(product.getColorCount())
                .priceRange(calculatePriceRange(product))

                // NEW FEATURES - Ordering Limits
                .orderingLimits(buildOrderingLimitsResponse(product))

                // NEW FEATURES - Group Buying
                .groupBuying(buildGroupBuyingResponse(product))

                // NEW FEATURES - Installment Options
                .installmentOptions(buildInstallmentOptionsResponse(product))

                // Purchase Options Summary
                .purchaseOptions(buildPurchaseOptionsResponse(product))

                .build();
    }

    public ProductSummaryResponse buildProductSummaryResponse(ProductEntity product) {
        // Get first image for card display
        String primaryImage = product.getProductImages() != null && !product.getProductImages().isEmpty()
                ? product.getProductImages().get(0)
                : null;

        // Calculate price variations
        PriceInfo priceInfo = calculatePriceInfo(product);

        return ProductSummaryResponse.builder()
                // Essential info
                .productId(product.getProductId())
                .productName(product.getProductName())
                .productSlug(product.getProductSlug())
                .shortDescription(product.getShortDescription())
                .primaryImage(primaryImage)

                // Pricing
                .price(product.getPrice())
                .comparePrice(product.getComparePrice())
                .finalPrice(priceInfo.getFinalPrice())
                .isOnSale(product.isOnSale())
                .discountPercentage(product.getDiscountPercentage())

                // Inventory
                .stockQuantity(product.getStockQuantity())
                .isInStock(product.isInStock())
                .isLowStock(product.isLowStock())

                // Basic details
                .brand(product.getBrand())
                .sku(product.getSku())
                .condition(product.getCondition())
                .status(product.getStatus())

                // Features
                .isFeatured(product.getIsFeatured())
                .isDigital(product.getIsDigital())

                // Special offers
                .hasGroupBuying(product.isGroupBuyingAvailable())
                .hasInstallments(product.isInstallmentAvailable())
                .hasMultipleColors(product.hasMultipleColors())
                .groupPrice(product.getGroupPrice())
                .startingFromPrice(priceInfo.getStartingFromPrice())

                // Timestamp
                .createdAt(product.getCreatedAt())
                .build();
    }


// ===============================
// COLOR RESPONSE BUILDER
// ===============================

    private List<ProductDetailedResponse.ColorDetailedResponse> buildColorResponses(List<Map<String, Object>> colors, BigDecimal basePrice) {
        if (colors == null || colors.isEmpty()) {
            return new ArrayList<>();
        }

        return colors.stream().map(colorMap -> {
            BigDecimal priceAdjustment = new BigDecimal(colorMap.getOrDefault("priceAdjustment", 0).toString());
            BigDecimal finalPrice = basePrice.add(priceAdjustment);

            return ProductDetailedResponse.ColorDetailedResponse.builder()
                    .name((String) colorMap.get("name"))
                    .hex((String) colorMap.get("hex"))
                    .images((List<String>) colorMap.getOrDefault("images", new ArrayList<>()))
                    .priceAdjustment(priceAdjustment)
                    .finalPrice(finalPrice)
                    .hasExtraFee(priceAdjustment.compareTo(BigDecimal.ZERO) > 0)
                    .extraFeeReason(priceAdjustment.compareTo(BigDecimal.ZERO) > 0 ? "Premium color finish" : null)
                    .build();
        }).toList();
    }

// ===============================
// PRICE RANGE CALCULATOR
// ===============================

    private ProductDetailedResponse.PriceRangeResponse calculatePriceRange(ProductEntity product) {
        BigDecimal minPrice = product.getPrice();
        BigDecimal maxPrice = product.getPrice();

        if (product.getColors() != null && !product.getColors().isEmpty()) {
            for (Map<String, Object> color : product.getColors()) {
                BigDecimal adjustment = new BigDecimal(color.getOrDefault("priceAdjustment", 0).toString());
                BigDecimal colorPrice = product.getPrice().add(adjustment);

                if (colorPrice.compareTo(minPrice) < 0) {
                    minPrice = colorPrice;
                }
                if (colorPrice.compareTo(maxPrice) > 0) {
                    maxPrice = colorPrice;
                }
            }
        }

        return ProductDetailedResponse.PriceRangeResponse.builder()
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .priceStartsFrom(minPrice)
                .hasPriceVariations(minPrice.compareTo(maxPrice) != 0)
                .build();
    }

// ===============================
// ORDERING LIMITS RESPONSE
// ===============================

    private ProductDetailedResponse.OrderingLimitsResponse buildOrderingLimitsResponse(ProductEntity product) {
        return ProductDetailedResponse.OrderingLimitsResponse.builder()
                .minOrderQuantity(product.getMinOrderQuantity())
                .maxOrderQuantity(product.getMaxOrderQuantity())
                .requiresApproval(product.getRequiresApproval())
                .canOrderQuantity(product.getMaxAllowedQuantity())
                .maxAllowedQuantity(product.getMaxAllowedQuantity())
                .hasOrderingLimits(product.getMaxOrderQuantity() != null ||
                        product.getMinOrderQuantity() > 1 ||
                        product.getRequiresApproval())
                .build();
    }

// ===============================
// GROUP BUYING RESPONSE
// ===============================

    private ProductDetailedResponse.OrderingLimitsResponse.GroupBuyingDetailedResponse buildGroupBuyingResponse(ProductEntity product) {
        if (!product.isGroupBuyingAvailable()) {
            return ProductDetailedResponse.OrderingLimitsResponse.GroupBuyingDetailedResponse.builder()
                    .isEnabled(false)
                    .isAvailable(false)
                    .build();
        }

        // In a real implementation, you'd fetch current group data from database
        // For now, we'll simulate some data
        int currentGroupSize = 7; // This would come from actual group buying records
        int remainingSlots = product.getGroupMaxSize() - currentGroupSize;
        double progressPercentage = ((double) currentGroupSize / product.getGroupMinSize()) * 100;

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(product.getGroupTimeLimitHours());
        long timeRemainingHours = product.getGroupTimeLimitHours() - 24; // Simulated

        return ProductDetailedResponse.OrderingLimitsResponse.GroupBuyingDetailedResponse.builder()
                .isEnabled(true)
                .isAvailable(currentGroupSize < product.getGroupMaxSize())
                .minGroupSize(product.getGroupMinSize())
                .maxGroupSize(product.getGroupMaxSize())
                .currentGroupSize(currentGroupSize)
                .remainingSlots(remainingSlots)
                .progressPercentage(Math.min(progressPercentage, 100.0))
                .groupPrice(product.getGroupPrice())
                .groupDiscount(product.getGroupDiscount())
                .groupDiscountPercentage(product.getGroupDiscountPercentage())
                .timeLimitHours(product.getGroupTimeLimitHours())
                .timeRemainingHours(Math.max(timeRemainingHours, 0))
                .expiresAt(expiresAt)
                .requiresMinimum(product.getGroupRequiresMinimum())
                .status(currentGroupSize >= product.getGroupMinSize() ? "ACTIVE" : "PENDING")
                .canJoinGroup(currentGroupSize < product.getGroupMaxSize())
                .build();
    }

// ===============================
// INSTALLMENT OPTIONS RESPONSE
// ===============================

    private ProductDetailedResponse.OrderingLimitsResponse.InstallmentOptionsDetailedResponse buildInstallmentOptionsResponse(ProductEntity product) {
        if (!product.isInstallmentAvailable()) {
            return ProductDetailedResponse.OrderingLimitsResponse.InstallmentOptionsDetailedResponse.builder()
                    .isEnabled(false)
                    .isAvailable(false)
                    .build();
        }

        List<ProductDetailedResponse.OrderingLimitsResponse.InstallmentPlanDetailedResponse> plans = product.getInstallmentPlans().stream()
                .map(planMap -> buildInstallmentPlanResponse(planMap, product))
                .toList();

        return ProductDetailedResponse.OrderingLimitsResponse.InstallmentOptionsDetailedResponse.builder()
                .isEnabled(true)
                .isAvailable(true)
                .downPaymentRequired(product.getDownPaymentRequired())
                .minDownPaymentPercentage(product.getMinDownPaymentPercentage())
                .plans(plans)
                .eligibilityStatus("ELIGIBLE") // This would be calculated based on user credit
                .creditCheckRequired(false)
                .build();
    }

    private ProductDetailedResponse.OrderingLimitsResponse.InstallmentPlanDetailedResponse buildInstallmentPlanResponse(Map<String, Object> planMap, ProductEntity product) {
        Integer duration = (Integer) planMap.get("duration");
        String interval = (String) planMap.get("interval");
        BigDecimal interestRate = new BigDecimal(planMap.getOrDefault("interestRate", 0).toString());
        String description = (String) planMap.get("description");

        // Calculate installment details
        ProductDetailedResponse.OrderingLimitsResponse.InstallmentCalculationResponse calculations = calculateInstallmentDetails(
                product.getPrice(),
                product.getMinDownPaymentPercentage(),
                duration,
                interval,
                interestRate
        );

        return ProductDetailedResponse.OrderingLimitsResponse.InstallmentPlanDetailedResponse.builder()
                .planId("plan-" + duration + interval.toLowerCase().substring(0, 1))
                .duration(duration)
                .interval(interval)
                .interestRate(interestRate)
                .description(description)
                .calculations(calculations)
                .paymentSchedule(generatePaymentSchedule(calculations, duration, interval))
                .isPopular(duration == 6 && "MONTHS".equals(interval)) // Mark 6 months as popular
                .build();
    }

// ===============================
// INSTALLMENT CALCULATION
// ===============================

    private ProductDetailedResponse.OrderingLimitsResponse.InstallmentCalculationResponse calculateInstallmentDetails(
            BigDecimal productPrice,
            BigDecimal downPaymentPercentage,
            Integer duration,
            String interval,
            BigDecimal interestRate) {

        // Calculate down payment
        BigDecimal downPayment = productPrice
                .multiply(downPaymentPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Remaining amount after down payment
        BigDecimal remainingAmount = productPrice.subtract(downPayment);

        // Calculate interest (simple interest for this example)
        BigDecimal totalInterest = remainingAmount
                .multiply(interestRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Total amount to pay in installments
        BigDecimal totalInstallmentAmount = remainingAmount.add(totalInterest);

        // Payment amount per interval
        BigDecimal paymentAmount = totalInstallmentAmount
                .divide(BigDecimal.valueOf(duration), 2, RoundingMode.HALF_UP);

        // Total amount (including down payment)
        BigDecimal totalAmount = productPrice.add(totalInterest);

        return ProductDetailedResponse.OrderingLimitsResponse.InstallmentCalculationResponse.builder()
                .downPayment(downPayment)
                .remainingAmount(remainingAmount)
                .totalInterest(totalInterest)
                .paymentAmount(paymentAmount)
                .totalAmount(totalAmount)
                .build();
    }

// ===============================
// PAYMENT SCHEDULE GENERATOR
// ===============================

    private List<ProductDetailedResponse.OrderingLimitsResponse.PaymentScheduleResponse> generatePaymentSchedule(
            ProductDetailedResponse.OrderingLimitsResponse.InstallmentCalculationResponse calculations,
            Integer duration,
            String interval) {

        List<ProductDetailedResponse.OrderingLimitsResponse.PaymentScheduleResponse> schedule = new ArrayList<>();
        LocalDateTime currentDate = LocalDateTime.now();

        for (int i = 1; i <= duration; i++) {
            LocalDateTime dueDate = calculateDueDate(currentDate, i, interval);

            schedule.add(ProductDetailedResponse.OrderingLimitsResponse.PaymentScheduleResponse.builder()
                    .paymentNumber(i)
                    .amount(calculations.getPaymentAmount())
                    .dueDate(dueDate)
                    .description(buildPaymentDescription(i, duration, interval))
                    .build());
        }

        return schedule;
    }

    private LocalDateTime calculateDueDate(LocalDateTime startDate, int paymentNumber, String interval) {
        return switch (interval) {
            case "DAYS" -> startDate.plusDays(paymentNumber * 1L);
            case "WEEKS" -> startDate.plusWeeks(paymentNumber * 1L);
            case "MONTHS" -> startDate.plusMonths(paymentNumber * 1L);
            default -> startDate.plusMonths(paymentNumber * 1L);
        };
    }

    private String buildPaymentDescription(int paymentNumber, int totalPayments, String interval) {
        if (paymentNumber == totalPayments) {
            return "Final payment";
        }

        return switch (interval) {
            case "DAYS" -> "Day " + paymentNumber + " payment";
            case "WEEKS" -> "Week " + paymentNumber + " payment";
            case "MONTHS" -> "Month " + paymentNumber + " payment";
            default -> "Payment " + paymentNumber;
        };
    }

// ===============================
// PURCHASE OPTIONS SUMMARY
// ===============================

    private ProductDetailedResponse.OrderingLimitsResponse.PurchaseOptionsResponse buildPurchaseOptionsResponse(ProductEntity product) {
        boolean canBuyNow = product.isInStock();
        boolean canJoinGroup = product.isGroupBuyingAvailable() && product.isInStock();
        boolean canPayInstallment = product.isInstallmentAvailable() && product.isInStock();

        // Determine recommended option based on savings
        String recommendedOption = "BUY_NOW";
        BigDecimal bestSavings = BigDecimal.ZERO;
        BigDecimal bestPrice = product.getPrice();

        if (canJoinGroup && product.getGroupDiscount().compareTo(BigDecimal.ZERO) > 0) {
            recommendedOption = "GROUP_BUYING";
            bestSavings = product.getGroupDiscount();
            bestPrice = product.getGroupPrice();
        }

        return ProductDetailedResponse.OrderingLimitsResponse.PurchaseOptionsResponse.builder()
                .canBuyNow(canBuyNow)
                .canJoinGroup(canJoinGroup)
                .canPayInstallment(canPayInstallment)
                .recommendedOption(recommendedOption)
                .bestDeal(ProductDetailedResponse.OrderingLimitsResponse.BestDealResponse.builder()
                        .option(recommendedOption)
                        .savings(bestSavings)
                        .finalPrice(bestPrice)
                        .build())
                .build();
    }

    // ===============================
    // SHOP SUMMARY BUILDER
    // ===============================

    public ProductSummaryResponse.ShopSummaryForProducts buildShopSummaryForProducts(ShopEntity shop) {
        return ProductSummaryResponse.ShopSummaryForProducts.builder()
                .shopId(shop.getShopId())
                .shopName(shop.getShopName())
                .shopSlug(shop.getShopSlug())
                .logoUrl(shop.getLogoUrl())
                .isVerified(shop.getIsVerified())
                .isApproved(shop.isApproved())
                .build();
    }

    // ===============================
    // PRODUCT LIST SUMMARY BUILDER
    // ===============================

    public ProductSummaryResponse.ProductListSummary buildProductListSummary(List<ProductEntity> products, ShopEntity shop) {
        if (products.isEmpty()) {
            return ProductSummaryResponse.ProductListSummary.builder()
                    .totalProducts(0)
                    .activeProducts(0)
                    .draftProducts(0)
                    .outOfStockProducts(0)
                    .featuredProducts(0)
                    .lowStockProducts(0)
                    .averagePrice(BigDecimal.ZERO)
                    .totalInventoryValue(BigDecimal.ZERO)
                    .productsWithGroupBuying(0)
                    .productsWithInstallments(0)
                    .productsWithMultipleColors(0)
                    .build();
        }

        // Calculate summary statistics
        int totalProducts = products.size();
        int activeProducts = (int) products.stream().filter(p -> p.getStatus() == ProductStatus.ACTIVE).count();
        int draftProducts = (int) products.stream().filter(p -> p.getStatus() == ProductStatus.DRAFT).count();
        int outOfStockProducts = (int) products.stream().filter(p -> !p.isInStock()).count();
        int featuredProducts = (int) products.stream().filter(p -> Boolean.TRUE.equals(p.getIsFeatured())).count();
        int lowStockProducts = (int) products.stream().filter(ProductEntity::isLowStock).count();

        // Special features count
        int productsWithGroupBuying = (int) products.stream().filter(ProductEntity::isGroupBuyingAvailable).count();
        int productsWithInstallments = (int) products.stream().filter(ProductEntity::isInstallmentAvailable).count();
        int productsWithMultipleColors = (int) products.stream().filter(ProductEntity::hasMultipleColors).count();

        // Calculate financial metrics
        BigDecimal totalValue = products.stream()
                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getStockQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averagePrice = products.stream()
                .map(ProductEntity::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(totalProducts), 2, RoundingMode.HALF_UP);

        return ProductSummaryResponse.ProductListSummary.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .draftProducts(draftProducts)
                .outOfStockProducts(outOfStockProducts)
                .featuredProducts(featuredProducts)
                .lowStockProducts(lowStockProducts)
                .averagePrice(averagePrice)
                .totalInventoryValue(totalValue)
                .productsWithGroupBuying(productsWithGroupBuying)
                .productsWithInstallments(productsWithInstallments)
                .productsWithMultipleColors(productsWithMultipleColors)
                .build();
    }

    // ===============================
    // HELPER METHODS
    // ===============================

    private PriceInfo calculatePriceInfo(ProductEntity product) {
        BigDecimal basePrice = product.getPrice();
        BigDecimal finalPrice = basePrice;
        BigDecimal startingFromPrice = basePrice;

        // Check if there are color price adjustments
        if (product.getColors() != null && !product.getColors().isEmpty()) {
            BigDecimal minPrice = basePrice;
            BigDecimal maxPrice = basePrice;

            for (Map<String, Object> color : product.getColors()) {
                BigDecimal adjustment = new BigDecimal(color.getOrDefault("priceAdjustment", 0).toString());
                BigDecimal colorPrice = basePrice.add(adjustment);

                if (colorPrice.compareTo(minPrice) < 0) {
                    minPrice = colorPrice;
                }
                if (colorPrice.compareTo(maxPrice) > 0) {
                    maxPrice = colorPrice;
                }
            }

            startingFromPrice = minPrice;
            finalPrice = minPrice; // Show lowest price for cards
        }

        return PriceInfo.builder()
                .finalPrice(finalPrice)
                .startingFromPrice(startingFromPrice)
                .build();
    }

    @Data
    @Builder
    private static class PriceInfo {
        private BigDecimal finalPrice;
        private BigDecimal startingFromPrice;
    }

    public ProductPublicResponse buildPublicProductResponse(ProductEntity product) {
        return ProductPublicResponse.builder()
                // Basic Information
                .productId(product.getProductId())
                .productName(product.getProductName())
                .productSlug(product.getProductSlug())
                .productDescription(product.getProductDescription())
                .shortDescription(product.getShortDescription())
                .productImages(product.getProductImages())

                // Pricing Information
                .price(product.getPrice())
                .comparePrice(product.getComparePrice())
                .discountAmount(product.getDiscountAmount())
                .discountPercentage(product.getDiscountPercentage())
                .isOnSale(product.isOnSale())

                // Availability Information (no exact stock numbers if low)
                .isInStock(product.isInStock())
                .isLowStock(product.isLowStock())
                .stockQuantity(product.isInStock() ? product.getStockQuantity() : null)

                // Product Details
                .brand(product.getBrand())
                .condition(product.getCondition())

                // SEO and Tags
                .tags(product.getTags())

                // Shop Information (minimal)
                .shopId(product.getShop().getShopId())
                .shopName(product.getShop().getShopName())
                .shopSlug(product.getShop().getShopSlug())
                .shopLogoUrl(product.getShop().getLogoUrl())

                // Category Information
                .categoryId(product.getCategory().getCategoryId())
                .categoryName(product.getCategory().getCategoryName())

                // Features
                .isDigital(product.getIsDigital())
                .requiresShipping(product.getRequiresShipping())

                // Specifications
                .specifications(product.getSpecifications())
                .hasSpecifications(product.hasSpecifications())

                // Colors
                .colors(buildPublicColorResponses(product.getColors(), product.getPrice()))
                .hasMultipleColors(product.hasMultipleColors())
                .priceRange(buildPublicPriceRange(product))

                // Special Offers
                .groupBuying(buildPublicGroupBuyingResponse(product))
                .installmentOptions(buildPublicInstallmentResponse(product))

                // Timestamp
                .createdAt(product.getCreatedAt())
                .build();
    }

    // Helper methods for building public nested responses
    private List<ProductPublicResponse.ProductColorPublicResponse> buildPublicColorResponses(List<Map<String, Object>> colors, BigDecimal basePrice) {
        if (colors == null || colors.isEmpty()) {
            return new ArrayList<>();
        }

        return colors.stream().map(colorMap -> {
            BigDecimal priceAdjustment = new BigDecimal(colorMap.getOrDefault("priceAdjustment", 0).toString());
            BigDecimal finalPrice = basePrice.add(priceAdjustment);

            return ProductPublicResponse.ProductColorPublicResponse.builder()
                    .name((String) colorMap.get("name"))
                    .hex((String) colorMap.get("hex"))
                    .images((List<String>) colorMap.getOrDefault("images", new ArrayList<>()))
                    .priceAdjustment(priceAdjustment)
                    .finalPrice(finalPrice)
                    .build();
        }).toList();
    }

    private ProductPublicResponse.PriceRangePublicResponse buildPublicPriceRange(ProductEntity product) {
        BigDecimal minPrice = product.getPrice();
        BigDecimal maxPrice = product.getPrice();

        if (product.getColors() != null && !product.getColors().isEmpty()) {
            for (Map<String, Object> color : product.getColors()) {
                BigDecimal adjustment = new BigDecimal(color.getOrDefault("priceAdjustment", 0).toString());
                BigDecimal colorPrice = product.getPrice().add(adjustment);

                if (colorPrice.compareTo(minPrice) < 0) {
                    minPrice = colorPrice;
                }
                if (colorPrice.compareTo(maxPrice) > 0) {
                    maxPrice = colorPrice;
                }
            }
        }

        return ProductPublicResponse.PriceRangePublicResponse.builder()
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .hasPriceVariations(minPrice.compareTo(maxPrice) != 0)
                .build();
    }

    private ProductPublicResponse.GroupBuyingPublicResponse buildPublicGroupBuyingResponse(ProductEntity product) {
        if (!product.isGroupBuyingAvailable()) {
            return ProductPublicResponse.GroupBuyingPublicResponse.builder()
                    .isAvailable(false)
                    .build();
        }

        return ProductPublicResponse.GroupBuyingPublicResponse.builder()
                .isAvailable(true)
                .minGroupSize(product.getGroupMinSize())
                .maxGroupSize(product.getGroupMaxSize())
                .groupPrice(product.getGroupPrice())
                .groupDiscount(product.getGroupDiscount())
                .groupDiscountPercentage(product.getGroupDiscountPercentage())
                .timeLimitHours(product.getGroupTimeLimitHours())
                .build();
    }

    private ProductPublicResponse.InstallmentPublicResponse buildPublicInstallmentResponse(ProductEntity product) {
        if (!product.isInstallmentAvailable()) {
            return ProductPublicResponse.InstallmentPublicResponse.builder()
                    .isAvailable(false)
                    .build();
        }

        List<ProductPublicResponse.InstallmentPlanPublicResponse> plans = product.getInstallmentPlans().stream()
                .map(planMap -> ProductPublicResponse.InstallmentPlanPublicResponse.builder()
                        .duration((Integer) planMap.get("duration"))
                        .interval((String) planMap.get("interval"))
                        .interestRate(new BigDecimal(planMap.getOrDefault("interestRate", 0).toString()))
                        .description((String) planMap.get("description"))
                        .build())
                .toList();

        return ProductPublicResponse.InstallmentPublicResponse.builder()
                .isAvailable(true)
                .plans(plans)
                .downPaymentRequired(product.getDownPaymentRequired())
                .minDownPaymentPercentage(product.getMinDownPaymentPercentage())
                .build();
    }
}

