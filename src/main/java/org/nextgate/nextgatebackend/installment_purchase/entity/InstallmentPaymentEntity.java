package org.nextgate.nextgatebackend.installment_purchase.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.nextgate.nextgatebackend.installment_purchase.enums.PaymentStatus;
import org.nextgate.nextgatebackend.payment_methods.utils.MetadataJsonConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "installment_payments", indexes = {
        @Index(name = "idx_payment_agreement", columnList = "agreement_id"),
        @Index(name = "idx_payment_status", columnList = "paymentStatus"),
        @Index(name = "idx_payment_due_date", columnList = "dueDate"),
        @Index(name = "idx_payment_number", columnList = "agreement_id, paymentNumber")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InstallmentPaymentEntity {

    // ========================================
    // PRIMARY IDENTIFICATION
    // ========================================

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id", referencedColumnName = "agreementId", nullable = false)
    @JsonIgnoreProperties({"payments", "hibernateLazyInitializer", "handler"})
    private InstallmentAgreementEntity agreement;

    @Column(nullable = false)
    private Integer paymentNumber;  // Sequential: 1, 2, 3, 4...

    // ========================================
    // AMOUNTS (Using Amortization)
    // ========================================

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal scheduledAmount;  // What customer should pay

    @Column(precision = 10, scale = 2)
    private BigDecimal paidAmount;  // What customer actually paid

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal principalPortion;  // How much goes toward principal

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal interestPortion;  // How much goes toward interest

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal remainingBalance;  // Balance after this payment

    @Column(precision = 10, scale = 2)
    private BigDecimal lateFee;  // If payment is late

    @Column(nullable = false, length = 10)
    private String currency = "TZS";

    // ========================================
    // STATUS
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    // ========================================
    // TIMING
    // ========================================

    @Column(nullable = false)
    private LocalDateTime dueDate;  // When payment should be made

    @Column(name = "paid_at")
    private LocalDateTime paidAt;  // When actually paid

    @Column(name = "attempted_at")
    private LocalDateTime attemptedAt;  // Last payment attempt

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // ========================================
    // PAYMENT DETAILS
    // ========================================

    @Column(length = 50)
    private String paymentMethod;  // "WALLET", "CREDIT_CARD", etc.

    @Column(name = "transaction_id", length = 100)
    private String transactionId;  // From payment gateway

    @Column(name = "checkout_session_id")
    private UUID checkoutSessionId;  // If manual payment via checkout

    @Column(name = "failure_reason", length = 500)
    private String failureReason;  // Why payment failed

    @Column(nullable = false)
    private Integer retryCount = 0;  // Number of retry attempts

    // ========================================
    // METADATA
    // ========================================

    @Column(columnDefinition = "TEXT")
    private String notes;  // Admin notes, customer requests

    @Column(name = "metadata", columnDefinition = "jsonb")
    @Convert(converter = MetadataJsonConverter.class)
    private Map<String, Object> metadata = new HashMap<>();

    // ========================================
    // LIFECYCLE HOOKS
    // ========================================

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ========================================
    // BUSINESS LOGIC METHODS
    // ========================================

    public boolean isDue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate);
    }

    public boolean isOverdue() {
        return isDue() && paymentStatus != PaymentStatus.COMPLETED;
    }

    public boolean isPaid() {
        return paymentStatus == PaymentStatus.COMPLETED;
    }

    public boolean canRetry() {
        return paymentStatus == PaymentStatus.FAILED && retryCount < 5;
    }

    public void recordSuccessfulPayment(String transactionId, String paymentMethod) {
        this.paymentStatus = PaymentStatus.COMPLETED;
        this.paidAt = LocalDateTime.now();
        this.paidAmount = this.scheduledAmount;
        this.transactionId = transactionId;
        this.paymentMethod = paymentMethod;
        this.failureReason = null;
    }

    public void recordFailedPayment(String reason) {
        this.paymentStatus = PaymentStatus.FAILED;
        this.attemptedAt = LocalDateTime.now();
        this.failureReason = reason;
        this.retryCount++;

        // Auto-mark as LATE if overdue
        if (isOverdue()) {
            this.paymentStatus = PaymentStatus.LATE;
        }
    }

    public void markAsLate() {
        if (paymentStatus != PaymentStatus.COMPLETED) {
            this.paymentStatus = PaymentStatus.LATE;
        }
    }

    public void markAsSkipped() {
        this.paymentStatus = PaymentStatus.SKIPPED;
    }

    public int getDaysOverdue() {
        if (!isOverdue()) return 0;

        long days = java.time.Duration.between(dueDate, LocalDateTime.now()).toDays();
        return (int) days;
    }
}