package org.nextgate.nextgatebackend.e_commerce.installment_purchase.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentPaymentDetailResponse {

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

    private BigDecimal remainingAmount;  // How much left to pay
    private Boolean isPartiallyPaid;     // Has some money applied
    private Double percentComplete;       // paidAmount / scheduledAmount * 100
}