package org.nextgate.nextgatebackend.installment_purchase.payloads;

import lombok.Data;
import org.nextgate.nextgatebackend.financial_system.payment_processing.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class InstallmentPaymentResponse {

    private UUID paymentId;
    private Integer paymentNumber;

    // Amounts
    private BigDecimal scheduledAmount;
    private BigDecimal paidAmount;
    private BigDecimal principalPortion;
    private BigDecimal interestPortion;
    private BigDecimal remainingBalance;
    private BigDecimal lateFee;
    private String currency;

    // Status
    private PaymentStatus paymentStatus;
    private String paymentStatusDisplay; // "Completed", "Pending", "Late"

    // Timing
    private LocalDateTime dueDate;
    private LocalDateTime paidAt;
    private LocalDateTime attemptedAt;

    // Payment details
    private String paymentMethod;
    private String transactionId;
    private String failureReason;
    private Integer retryCount;

    // Days info
    private Integer daysUntilDue; // Calculated: dueDate - today
    private Integer daysOverdue; // If late

    // Actions
    private Boolean canPay;
    private Boolean canRetry;
}