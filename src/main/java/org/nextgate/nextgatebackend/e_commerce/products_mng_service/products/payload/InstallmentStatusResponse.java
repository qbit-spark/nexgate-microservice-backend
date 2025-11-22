package org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentStatusResponse {

    // ========================================
    // PRODUCT INFO
    // ========================================
    private UUID productId;
    private String productName;
    private BigDecimal productPrice;

    // ========================================
    // INSTALLMENT STATUS
    // ========================================
    private Boolean installmentEnabled;
    private Integer maxQuantityForInstallment;

    // ========================================
    // PLAN STATISTICS
    // ========================================
    private Integer totalPlans;
    private Long activePlans;
    private Long inactivePlans;

    // ========================================
    // FEATURED PLAN
    // ========================================
    private FeaturedPlanInfo featuredPlan;

    // ========================================
    // AVAILABILITY
    // ========================================
    private Boolean canEnableInstallments;
    private String message;

    // ========================================
    // NESTED CLASS: Featured Plan Info
    // ========================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeaturedPlanInfo {
        private UUID planId;
        private String planName;
        private Integer numberOfPayments;
        private BigDecimal apr;
        private String calculatedDurationDisplay;
    }
}