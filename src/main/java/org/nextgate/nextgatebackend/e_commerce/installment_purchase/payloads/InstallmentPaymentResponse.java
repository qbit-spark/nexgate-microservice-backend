package org.nextgate.nextgatebackend.e_commerce.installment_purchase.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.financial_system.payment_processing.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentPaymentResponse {

    private UUID paymentId;
    private Integer paymentNumber;

    private BigDecimal scheduledAmount;
    private BigDecimal paidAmount;
    private BigDecimal principalPortion;
    private BigDecimal interestPortion;
    private BigDecimal remainingBalance;
    private BigDecimal lateFee;
    private String currency;

    private PaymentStatus paymentStatus;
    private String paymentStatusDisplay;

    private LocalDateTime dueDate;
    private LocalDateTime paidAt;
    private LocalDateTime attemptedAt;

    private String paymentMethod;
    private String transactionId;
    private String failureReason;
    private Integer retryCount;

    private Integer daysUntilDue;
    private Integer daysOverdue;

    private Boolean canPay;
    private Boolean canRetry;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgreementUpdate {
        private Integer paymentsCompleted;
        private Integer paymentsRemaining;
        private BigDecimal amountPaid;
        private BigDecimal amountRemaining;
        private LocalDateTime nextPaymentDate;
        private BigDecimal nextPaymentAmount;
        private String agreementStatus;
        private Boolean isCompleted;
    }
}