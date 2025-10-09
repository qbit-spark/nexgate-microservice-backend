package org.nextgate.nextgatebackend.installment_purchase.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentResponse {

    private UUID paymentId;
    private UUID agreementId;
    private String agreementNumber;

    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String transactionId;

    private String status;
    private LocalDateTime processedAt;

    private String message;

    private AgreementUpdate agreementUpdate;

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