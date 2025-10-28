package org.nextgate.nextgatebackend.installment_purchase.payloads;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlexiblePaymentPreviewResponse {

    private BigDecimal requestedAmount;
    private BigDecimal minimumRequired;  // Minimum next payment
    private BigDecimal maximumAllowed;   // Total remaining

    private boolean isValid;
    private String validationMessage;

    // Show what will happen
    private List<PaymentImpactPreview> impactedPayments;

    private Integer paymentsWillComplete;
    private Integer paymentsWillBePartial;
    private BigDecimal remainingAfter;

    @Data
    @Builder
    public static class PaymentImpactPreview {
        private Integer paymentNumber;
        private LocalDateTime dueDate;
        private BigDecimal scheduledAmount;
        private BigDecimal currentPaid;
        private BigDecimal willApply;
        private BigDecimal willRemain;
        private String resultStatus;  // "Will be COMPLETED" or "Will be PARTIALLY_PAID"
    }
}