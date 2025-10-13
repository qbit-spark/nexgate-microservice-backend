package org.nextgate.nextgatebackend.checkout_session.entity;

import jakarta.persistence.*;
import lombok.*;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionType;
import org.nextgate.nextgatebackend.checkout_session.utils.*;
import org.nextgate.nextgatebackend.payment_methods.utils.BillingAddressJsonConverter;
import org.nextgate.nextgatebackend.payment_methods.utils.MetadataJsonConverter;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "checkout_sessions", indexes = {
        @Index(name = "idx_session_customer", columnList = "customer_id"),
        @Index(name = "idx_session_status", columnList = "status"),
        @Index(name = "idx_session_type", columnList = "sessionType"),
        @Index(name = "idx_session_expires", columnList = "expiresAt")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckoutSessionType sessionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id", nullable = false)
    private AccountEntity customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckoutSessionStatus status;

    // Items in checkout (stored as JSON)
    @Column(name = "items", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = CheckoutItemsJsonConverter.class)
    private List<CheckoutItem> items = new ArrayList<>();

    // Pricing summary (stored as JSON)
    @Column(name = "pricing", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = PricingSummaryJsonConverter.class)
    private PricingSummary pricing;

    // Shipping address (stored as JSON)
    @Column(name = "shipping_address", columnDefinition = "jsonb")
    @Convert(converter = ShippingAddressJsonConverter.class)
    private ShippingAddress shippingAddress;

    // Billing address (stored as JSON)
    @Column(name = "billing_address", columnDefinition = "jsonb")
    @Convert(converter = BillingAddressJsonConverter.class)
    private BillingAddress billingAddress;

    // Shipping method (stored as JSON)
    @Column(name = "shipping_method", columnDefinition = "jsonb")
    @Convert(converter = ShippingMethodJsonConverter.class)
    private ShippingMethod shippingMethod;

    // Payment intent details (stored as JSON)
    @Column(name = "payment_intent", columnDefinition = "jsonb")
    @Convert(converter = PaymentIntentJsonConverter.class)
    private PaymentIntent paymentIntent;

    // Payment attempts tracking (stored as JSON)
    @Column(name = "payment_attempts", columnDefinition = "jsonb")
    @Convert(converter = PaymentAttemptsJsonConverter.class)
    private List<PaymentAttempt> paymentAttempts = new ArrayList<>();

    // Inventory hold information
    @Column(name = "inventory_held")
    private Boolean inventoryHeld = false;

    @Column(name = "inventory_hold_expires_at")
    private LocalDateTime inventoryHoldExpiresAt;

    // Metadata (stored as JSON) - for coupons, referrals, etc.
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Convert(converter = MetadataJsonConverter.class)
    private Map<String, Object> metadata = new HashMap<>();

    // Session timing
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;


    @Column(name = "created_order_ids", columnDefinition = "jsonb")
    @Convert(converter = OrderIdsJsonConverter.class)
    private List<UUID> createdOrderIds;

    private UUID groupIdToBeJoined; // For GROUP_PURCHASE type

    @Column(name = "selected_installment_plan_id")
    private UUID selectedInstallmentPlanId;

    @Column(name = "installment_config", columnDefinition = "jsonb")
    @Convert(converter = InstallmentConfigJsonConverter.class)
    private InstallmentConfiguration installmentConfig;


    // Cart reference (for REGULAR_CART type)
    @Column(name = "cart_id")
    private UUID cartId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Set default expiration (15 minutes from now)
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMinutes(15);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business logic methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean canRetryPayment() {
        return status == CheckoutSessionStatus.PAYMENT_FAILED && !isExpired();
    }

    public void addPaymentAttempt(PaymentAttempt attempt) {
        if (paymentAttempts == null) {
            paymentAttempts = new ArrayList<>();
        }
        paymentAttempts.add(attempt);
    }

    public int getPaymentAttemptCount() {
        return paymentAttempts != null ? paymentAttempts.size() : 0;
    }


    // Helper methods for easier access
    public UUID getPrimaryOrderId() {
        return (createdOrderIds != null && !createdOrderIds.isEmpty())
                ? createdOrderIds.get(0)
                : null;
    }

    public void addCreatedOrderId(UUID orderId) {
        if (this.createdOrderIds == null) {
            this.createdOrderIds = new ArrayList<>();
        }
        this.createdOrderIds.add(orderId);
    }

    public void setCreatedOrderIds(List<UUID> orderIds) {
        this.createdOrderIds = (orderIds != null) ? orderIds : new ArrayList<>();
    }

    // ========================================
    // NESTED CLASSES FOR JSON STORAGE
    // ========================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutItem {
       private UUID productId;
        private String productName;
        private String productSlug;
        private String productImage;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
        private BigDecimal tax;
        private BigDecimal total;
        private UUID shopId;
        private String shopName;
        private String shopLogo;
        private Boolean availableForCheckout;
        private Integer availableQuantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricingSummary {
        private BigDecimal subtotal;
        private BigDecimal shippingCost;
        private BigDecimal tax;
        private BigDecimal total;
        private String currency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingAddress {
        private String fullName;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingAddress {
        private Boolean sameAsShipping;
        private String fullName;
        private String addressLine1;
        private String city;
        private String state;
        private String postalCode;
        private String country;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingMethod {
        private String id;
        private String name;
        private String carrier;
        private BigDecimal cost;
        private String estimatedDays;
        private String estimatedDelivery;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentIntent {
        private String provider;
        private String clientSecret;
        private List<String> paymentMethods;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentAttempt {
        private Integer attemptNumber;
        private String paymentMethod;
        private String status;
        private String errorMessage;
        private LocalDateTime attemptedAt;
        private String transactionId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstallmentConfiguration {
        private UUID planId;
        private String planName;
        private Integer termMonths;
        private BigDecimal apr;

        // Down payment
        private Integer downPaymentPercent;      // Customer chose (e.g., 25%)
        private BigDecimal downPaymentAmount;

        // Financing
        private BigDecimal financedAmount;       // After down payment
        private BigDecimal monthlyPaymentAmount; // Per installment
        private BigDecimal totalInterest;
        private BigDecimal totalAmount;          // Grand total

        // Timing
        private LocalDateTime firstPaymentDate;
        private Integer gracePeriodDays;
        private String fulfillmentTiming;        // IMMEDIATE or AFTER_PAYMENT

        // Full schedule preview
        private List<PaymentScheduleItem> schedule;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentScheduleItem {
        private Integer paymentNumber;
        private LocalDateTime dueDate;
        private BigDecimal amount;
        private BigDecimal principalPortion;
        private BigDecimal interestPortion;
        private BigDecimal remainingBalance;
    }
}