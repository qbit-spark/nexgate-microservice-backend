package org.nextgate.nextgatebackend.e_commerce.installment_purchase.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.AgreementStatus;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.FulfillmentTiming;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.PaymentFrequency;
import org.nextgate.nextgatebackend.payment_methods.utils.MetadataJsonConverter;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.entity.ShopEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "installment_agreements", indexes = {
        @Index(name = "idx_agreement_customer", columnList = "customer_id"),
        @Index(name = "idx_agreement_product", columnList = "product_id"),
        @Index(name = "idx_agreement_shop", columnList = "shop_id"),
        @Index(name = "idx_agreement_status", columnList = "agreementStatus"),
        @Index(name = "idx_agreement_next_payment", columnList = "nextPaymentDate"),
        @Index(name = "idx_agreement_number", columnList = "agreementNumber")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InstallmentAgreementEntity {

    // ========================================
    // PRIMARY IDENTIFICATION
    // ========================================

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID agreementId;

    @Column(unique = true, nullable = false, length = 50)
    private String agreementNumber;  // Format: "INST-2025-00789"

    // ========================================
    // RELATIONSHIPS
    // ========================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private AccountEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "productId", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", referencedColumnName = "shopId", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ShopEntity shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_plan_id", referencedColumnName = "planId", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private InstallmentPlanEntity selectedPlan;  // Reference to template (for history)

    @Column(name = "checkout_session_id", nullable = false)
    private UUID checkoutSessionId;  // The checkout that created this agreement

    @OneToMany(mappedBy = "agreement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InstallmentPaymentEntity> payments = new ArrayList<>();

    // ========================================
    // PRODUCT SNAPSHOT (Immutable at creation)
    // ========================================

    @Column(nullable = false, length = 255)
    private String productName;

    @Column(length = 500)
    private String productImage;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal productPrice;

    @Column(nullable = false)
    private Integer quantity = 1;  // Default 1

    // ========================================
    // PAYMENT TERMS SNAPSHOT (From plan at creation)
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentFrequency paymentFrequency;

    @Column(name = "custom_frequency_days")
    private Integer customFrequencyDays;  // Nullable

    @Column(nullable = false)
    private Integer numberOfPayments;

    @Column(nullable = false)
    private Integer termMonths;  // Approximate duration in months

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal apr;  // Snapshot of APR at creation

    @Column(name = "payment_start_delay_days", nullable = false)
    private Integer paymentStartDelayDays;

    // ========================================
    // FINANCIAL BREAKDOWN
    // ========================================

    @Column(nullable = false)
    private Integer downPaymentPercent;  // Actual percent customer chose

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal downPaymentAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal financedAmount;  // Amount being financed with interest

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPaymentAmount;  // Each installment amount

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalInterestAmount;  // Total interest to be paid

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;  // Grand total (down + all installments)

    @Column(nullable = false, length = 10)
    private String currency = "TZS";

    // ========================================
    // PAYMENT TRACKING
    // ========================================

    @Column(nullable = false)
    private Integer paymentsCompleted = 0;  // How many payments made

    @Column(nullable = false)
    private Integer paymentsRemaining;  // How many left

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid;  // Running total paid so far

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountRemaining;  // Still owed

    @Column(name = "next_payment_date")
    private LocalDateTime nextPaymentDate;  // When next payment is due

    @Column(name = "next_payment_amount", precision = 10, scale = 2)
    private BigDecimal nextPaymentAmount;  // Amount of next payment

    // ========================================
    // STATUS
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgreementStatus agreementStatus;

    @Column(nullable = false)
    private Integer defaultCount = 0;  // Number of missed payments

    @Column(nullable = false)
    private Integer consecutiveLatePayments = 0;  // Track patterns

    // ========================================
    // TIMING
    // ========================================

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime firstPaymentDate;  // When first installment due

    @Column(nullable = false)
    private LocalDateTime lastPaymentDate;  // When final payment due

    @Column(name = "completed_at")
    private LocalDateTime completedAt;  // When fully paid

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========================================
    // FULFILLMENT
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FulfillmentTiming fulfillmentTiming;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "order_id")
    private UUID orderId;  // Link to created order

    // ========================================
    // SHIPPING/BILLING (Store as text for now, can convert to JSONB later)
    // ========================================

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;  // Can be JSONB if needed

    @Column(name = "billing_address", columnDefinition = "TEXT")
    private String billingAddress;  // Can be JSONB if needed

    // ========================================
    // METADATA
    // ========================================

    @Column(name = "metadata", columnDefinition = "jsonb")
    @Convert(converter = MetadataJsonConverter.class)
    private Map<String, Object> metadata = new HashMap<>();

    // ========================================
    // SOFT DELETE
    // ========================================

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "delete_reason", length = 500)
    private String deleteReason;

    // ========================================
    // LIFECYCLE HOOKS
    // ========================================

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (agreementNumber == null) {
            agreementNumber = generateAgreementNumber();
        }
        if (paymentsRemaining == null && numberOfPayments != null) {
            paymentsRemaining = numberOfPayments;
        }
        if (amountPaid == null) {
            amountPaid = downPaymentAmount;  // Down payment already paid
        }
        if (amountRemaining == null && totalAmount != null) {
            amountRemaining = totalAmount.subtract(downPaymentAmount);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========================================
    // BUSINESS LOGIC METHODS
    // ========================================

    private String generateAgreementNumber() {
        // Format: INST-YYYY-NNNNN
        int year = LocalDateTime.now().getYear();
        String randomPart = String.format("%05d", new Random().nextInt(100000));
        return String.format("INST-%d-%s", year, randomPart);
    }

    public boolean isActive() {
        return agreementStatus == AgreementStatus.ACTIVE;
    }

    public boolean isCompleted() {
        return agreementStatus == AgreementStatus.COMPLETED;
    }

    public boolean isDefaulted() {
        return agreementStatus == AgreementStatus.DEFAULTED;
    }

    public boolean hasOverduePayments() {
        return defaultCount > 0;
    }

    public boolean shouldShipNow() {
        return fulfillmentTiming == FulfillmentTiming.IMMEDIATE
                && shippedAt == null;
    }

    public boolean canShipProduct() {
        if (fulfillmentTiming == FulfillmentTiming.IMMEDIATE) {
            return true;  // Ship after down payment
        }
        // AFTER_FIRST_PAYMENT: ship only if completed
        return isCompleted();
    }

    public BigDecimal getProgressPercentage() {
        if (numberOfPayments == null || numberOfPayments == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(paymentsCompleted)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(numberOfPayments), 2, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal getAmountPaidPercentage() {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amountPaid.multiply(BigDecimal.valueOf(100))
                .divide(totalAmount, 2, BigDecimal.ROUND_HALF_UP);
    }

    public void recordPayment(BigDecimal amount) {
        this.paymentsCompleted++;
        this.paymentsRemaining--;
        this.amountPaid = this.amountPaid.add(amount);
        this.amountRemaining = this.amountRemaining.subtract(amount);
        this.consecutiveLatePayments = 0;  // Reset on successful payment

        if (paymentsRemaining == 0) {
            this.agreementStatus = AgreementStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();
        }
    }

    public void recordMissedPayment() {
        this.defaultCount++;
        this.consecutiveLatePayments++;

        // Auto-default after 2 missed payments
        if (defaultCount >= 2) {
            this.agreementStatus = AgreementStatus.DEFAULTED;
        }
    }
}