package org.nextgate.nextgatebackend.e_commerce.installment_purchase.payloads;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class FlexiblePaymentResponse {

    private UUID agreementId;
    private String agreementNumber;

    private BigDecimal totalAmountPaid;
    private String currency;
    private String transactionId;
    private LocalDateTime processedAt;

    // Summary of affected payments
    private List<PaymentApplicationDetail> paymentsAffected;

    // Agreement status after payment
    private AgreementUpdateSummary agreementUpdate;

    private String message;  // "Successfully paid 2.5 installments"

    @Data
    @Builder
    public static class PaymentApplicationDetail {
        private UUID paymentId;
        private Integer paymentNumber;
        private LocalDateTime dueDate;

        private BigDecimal scheduledAmount;
        private BigDecimal amountApplied;  // How much of flexible payment went here
        private BigDecimal previouslyPaid;
        private BigDecimal newPaidAmount;
        private BigDecimal remaining;

        private String status;  // "COMPLETED", "PARTIALLY_PAID"
        private boolean wasCompleted;  // Did this payment get fully paid?
    }

    @Data
    @Builder
    public static class AgreementUpdateSummary {
        private Integer paymentsCompleted;
        private Integer paymentsPartial;  // NEW: Partially paid count
        private Integer paymentsRemaining;
        private BigDecimal amountPaid;
        private BigDecimal amountRemaining;
        private LocalDateTime nextPaymentDate;
        private BigDecimal nextPaymentAmount;
        private String agreementStatus;
        private Boolean isCompleted;
    }
}