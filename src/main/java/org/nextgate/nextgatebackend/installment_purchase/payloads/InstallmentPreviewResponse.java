package org.nextgate.nextgatebackend.installment_purchase.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO containing full installment preview calculations
 * Shows customer exactly what they'll pay before checkout
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentPreviewResponse {

    // ========================================
    // PLAN INFORMATION
    // ========================================

    private UUID planId;
    private String planName;
    private String planDescription;

    // ========================================
    // PAYMENT SCHEDULE INFO
    // ========================================

    private String paymentFrequency;        // "Monthly", "Bi-weekly"
    private Integer numberOfPayments;       // 6, 12, etc.
    private String durationDisplay;         // "6 months", "12 weeks"
    private BigDecimal apr;                 // 15.00
    private Integer gracePeriodDays;        // 30

    // ========================================
    // PRODUCT INFO
    // ========================================

    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal totalProductCost;    // productPrice × quantity

    // ========================================
    // DOWN PAYMENT
    // ========================================

    private Integer downPaymentPercent;
    private BigDecimal downPaymentAmount;
    private Integer minDownPaymentPercent;
    private Integer maxDownPaymentPercent;
    private BigDecimal minDownPaymentAmount;
    private BigDecimal maxDownPaymentAmount;

    // ========================================
    // FINANCIAL BREAKDOWN
    // ========================================

    private BigDecimal financedAmount;          // Amount being financed
    private BigDecimal monthlyPaymentAmount;    // Each installment
    private BigDecimal totalInterestAmount;     // Total interest to pay
    private BigDecimal totalAmount;             // Grand total (product + interest)
    private String currency;                    // "TZS"

    // ========================================
    // DATES
    // ========================================

    private LocalDateTime firstPaymentDate;
    private LocalDateTime lastPaymentDate;

    // ========================================
    // PAYMENT SCHEDULE (Full breakdown)
    // ========================================

    private List<PaymentSchedulePreview> schedule;

    // ========================================
    // COMPARISON (vs full price)
    // ========================================

    private ComparisonInfo comparison;

    // ========================================
    // FULFILLMENT INFO
    // ========================================

    private String fulfillmentTiming;       // "IMMEDIATE" or "AFTER_PAYMENT"
    private String fulfillmentDescription;  // Human-readable

    // ========================================
    // NESTED CLASSES
    // ========================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSchedulePreview {
        private Integer paymentNumber;
        private LocalDateTime dueDate;
        private BigDecimal amount;
        private BigDecimal principalPortion;
        private BigDecimal interestPortion;
        private BigDecimal remainingBalance;
        private String description;             // "Month 1 payment", "Final payment"
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonInfo {
        private BigDecimal payingUpfront;       // Product price (if buying now)
        private BigDecimal payingWithInstallment; // Total with interest
        private BigDecimal additionalCost;      // Interest amount
        private BigDecimal additionalCostPercent; // Interest as % of product price
    }
}