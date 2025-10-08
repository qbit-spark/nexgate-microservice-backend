package org.nextgate.nextgatebackend.products_mng_service.products.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.installment_purchase.enums.FulfillmentTiming;
import org.nextgate.nextgatebackend.installment_purchase.enums.PaymentFrequency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentPlanResponse {

    // Plan identification
    private UUID planId;
    private String planName;
    private Integer displayOrder;

    // Payment terms
    private PaymentFrequency paymentFrequency;
    private Integer customFrequencyDays;
    private Integer numberOfPayments;
    private BigDecimal apr;
    private Integer minDownPaymentPercent;
    private Integer gracePeriodDays;

    // Calculated duration
    private Integer calculatedDurationDays;
    private String calculatedDurationDisplay;

    // Fulfillment & status
    private FulfillmentTiming fulfillmentTiming;
    private Boolean isActive;
    private Boolean isFeatured;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Nested product info
    private ProductBasicInfo product;

    // ========================================
    // NESTED CLASS: Product Basic Info
    // ========================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductBasicInfo {
        private UUID productId;
        private String productName;
        private String productSlug;
        private BigDecimal productPrice;
        private String primaryImage;
    }
}