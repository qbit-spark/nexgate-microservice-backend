package org.nextgate.nextgatebackend.e_commerce.installment_purchase.payloads;

import lombok.Data;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.FulfillmentTiming;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.PaymentFrequency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Data
public class InstallmentPlanResponse {

    private UUID planId;
    private String planName;

    // Schedule
    private PaymentFrequency paymentFrequency;
    private String paymentFrequencyDisplay;
    private Integer customFrequencyDays;
    private Integer numberOfPayments;
    private String duration;

    // Terms
    private BigDecimal apr;
    private Integer minDownPaymentPercent;
    private Integer paymentStartDelayDays;
    private FulfillmentTiming fulfillmentTiming;

    // Status
    private Boolean isActive;
    private Boolean isFeatured;
    private Integer displayOrder;

    // Preview calculations (based on product price)
    private InstallmentPreview preview;

    @Data
    public static class InstallmentPreview {
        private BigDecimal productPrice;
        private BigDecimal minDownPaymentAmount;
        private BigDecimal maxDownPaymentAmount;
        private BigDecimal financedAmountExample; // At min down
        private BigDecimal paymentAmountExample; // At min down
        private BigDecimal totalInterestExample;
        private BigDecimal totalCostExample;
        private LocalDateTime firstPaymentDateExample;
        private LocalDateTime lastPaymentDateExample;
    }
}