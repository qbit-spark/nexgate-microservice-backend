package org.nextgate.nextgatebackend.e_commerce.checkout_session.entity;


import org.nextgate.nextgatebackend.financial_system.payment_processing.contract.PayableCheckoutSession;
import jakarta.persistence.*;
import lombok.*;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionType;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.utils.*;
import org.nextgate.nextgatebackend.financial_system.payment_processing.utils.PaymentAttemptsJsonConverter;
import org.nextgate.nextgatebackend.financial_system.payment_processing.utils.PricingDetailsJsonConverter;
import org.nextgate.nextgatebackend.globe_enums.CheckoutSessionsDomains;
import org.nextgate.nextgatebackend.payment_methods.utils.BillingAddressJsonConverter;
import org.nextgate.nextgatebackend.payment_methods.utils.MetadataJsonConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Product Checkout Session Entity
 *
 * Represents a checkout session for e-commerce product purchases.
 * This is part of the PRODUCT DOMAIN and handles various purchase types.
 *
 * Supported Purchase Types:
 * - REGULAR_DIRECTLY: Direct product purchase (buy now)
 * - REGULAR_CART: Cart-based purchase (multiple items)
 * - INSTALLMENT: Installment payment plan
 * - GROUP_PURCHASE: Group buying deals
 *
 * Implements PayableCheckoutSession to enable payment processing
 * through the universal payment orchestrator.
 *
 * Payment Flow:
 * 1. Customer creates checkout session
 * 2. Session holds inventory (if applicable)
 * 3. Payment is processed via PaymentOrchestrator
 * 4. Money flows: Payer → Escrow → Payee (shop owner)
 * 5. Order is created after successful payment
 *
 */
@Entity
@Table(name = "product_checkout_sessions",
        indexes = {
                @Index(name = "idx_product_checkout_customer", columnList = "customer_id"),
                @Index(name = "idx_product_checkout_status", columnList = "status"),
                @Index(name = "idx_product_checkout_session_type", columnList = "session_type"),
                @Index(name = "idx_product_checkout_expires", columnList = "expires_at"),
                @Index(name = "idx_product_checkout_created", columnList = "created_at"),
                @Index(name = "idx_product_checkout_escrow", columnList = "escrow_id"),
                @Index(name = "idx_product_checkout_order", columnList = "order_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCheckoutSessionEntity implements PayableCheckoutSession {

    // ========================================
    // PRIMARY KEY
    // ========================================

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID sessionId;

    // ========================================
    // PAYMENT PARTICIPANT (PAYER)
    // ========================================

    /**
     * The customer making the purchase (PAYER in payment flow)
     * Money flows FROM this account
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id", nullable = false)
    private AccountEntity customer;

    // ========================================
    // SESSION TYPE (PRODUCT DOMAIN)
    // ========================================

    /**
     * Type of product checkout:
     * - REGULAR_DIRECTLY: Direct purchase
     * - REGULAR_CART: Cart-based purchase
     * - INSTALLMENT: Pay in installments
     * - GROUP_PURCHASE: Group buying deal
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false)
    private CheckoutSessionType sessionType;

    // ========================================
    // SESSION STATUS
    // ========================================

    /**
     * Current status of the checkout session
     * Lifecycle: PENDING_PAYMENT → PAYMENT_PROCESSING → PAYMENT_COMPLETED
     *           or → PAYMENT_FAILED → EXPIRED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CheckoutSessionStatus status = CheckoutSessionStatus.PENDING_PAYMENT;

    // ========================================
    // PRODUCT ITEMS
    // ========================================

    /**
     * List of products in this checkout
     * Stored as JSONB for flexibility
     */
    @Column(name = "items", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = CheckoutItemsJsonConverter.class)
    private List<CheckoutItem> items;

    // ========================================
    // PRICING DETAILS
    // ========================================

    /**
     * Comprehensive pricing breakdown
     * Includes subtotal, fees, discounts, and total
     * Stored as JSONB for flexibility
     */
    @Column(name = "pricing", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = PricingDetailsJsonConverter.class)
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





    // ========================================
    // PAYMENT INTENT
    // ========================================

    /**
     * Payment provider information
     * Contains payment method, client secrets, etc.
     * Stored as JSONB
     */
    @Column(name = "payment_intent", columnDefinition = "jsonb")
    @Convert(converter = ProductPaymentIntentJsonConverter.class)
    private PaymentIntent paymentIntent;

    // ========================================
    // PAYMENT ATTEMPTS TRACKING
    // ========================================

    /**
     * History of payment attempts
     * Tracks retries, failures, and success
     * Stored as JSONB array
     */
    @Column(name = "payment_attempts", columnDefinition = "jsonb")
    @Convert(converter = PaymentAttemptsJsonConverter.class)
    @Builder.Default
    private List<PaymentAttempt> paymentAttempts = new ArrayList<>();

    // ========================================
    // INVENTORY MANAGEMENT
    // ========================================

    /**
     * Whether inventory has been held for this session
     * Prevents overselling during checkout process
     */
    @Column(name = "inventory_held")
    @Builder.Default
    private Boolean inventoryHeld = false;

    /**
     * When the inventory hold expires
     * Inventory is released if session expires or fails
     */
    @Column(name = "inventory_hold_expires_at")
    private LocalDateTime inventoryHoldExpiresAt;



    // Metadata (stored as JSON) - for coupons, referrals, etc.
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Convert(converter = MetadataJsonConverter.class)
    private Map<String, Object> metadata = new HashMap<>();

    // ========================================
    // SESSION TIMING
    // ========================================

    /**
     * When this session expires (cannot be used after this time)
     * Default: 30 minutes after creation
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * When this session was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * When payment was completed successfully
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;



    @Column(name = "created_order_ids", columnDefinition = "jsonb")
    @Convert(converter = OrderIdsJsonConverter.class)
    private List<UUID> createdOrderIds = new ArrayList<>(); // ✅ Mutable

    // ========================================
    // INSTALLMENT-SPECIFIC FIELDS
    // ========================================

    /**
     * Selected installment plan ID (if sessionType = INSTALLMENT)
     */
    @Column(name = "selected_installment_plan_id")
    private UUID selectedInstallmentPlanId;


    @Column(name = "installment_config", columnDefinition = "jsonb")
    @Convert(converter = InstallmentConfigJsonConverter.class)
    private InstallmentConfiguration installmentConfig;


    // Cart reference (for REGULAR_CART type)
    @Column(name = "cart_id")
    private UUID cartId;

    // ========================================
    // GROUP PURCHASE-SPECIFIC FIELDS
    // ========================================

    /**
     * Group instance ID to join (if sessionType = GROUP_PURCHASE)
     * If null, a new group will be created
     */
    @Column(name = "group_id_to_be_joined")
    private UUID groupIdToBeJoined;


    // ========================================
    // PAYMENT RESULT REFERENCES
    // ========================================

    /**
     * Escrow account ID created after successful payment
     * Links to the escrow that holds the funds
     */
    @Column(name = "escrow_id")
    private UUID escrowId;

    /**
     * Order ID created after payment completion
     * Links to the final order entity
     */
    @Column(name = "order_id")
    private UUID orderId;

    // ========================================
    // METADATA
    // ========================================

    /**
     * Optional notes or additional information
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ========================================
    // LIFECYCLE CALLBACKS
    // ========================================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Set default expiration (30 minutes from now)
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMinutes(30);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========================================
    // PAYMENT CONTRACT IMPLEMENTATION
    // ========================================

    /**
     * Returns the payer (source of funds)
     * In product checkout, this is the customer
     */
    @Override
    public AccountEntity getPayer() {
        return customer;
    }

    /**
     * Returns the session domain identifier
     * Used by payment system to route domain-specific logic
     */
    @Override
    public CheckoutSessionsDomains getSessionDomain() {
        return CheckoutSessionsDomains.PRODUCT;
    }

    /**
     * Returns the total amount to be paid
     */
    @Override
    public BigDecimal getTotalAmount() {
        return pricing != null ? pricing.getTotal() : BigDecimal.ZERO;
    }

    /**
     * Returns the currency code
     */
    @Override
    public String getCurrency() {
        return pricing != null ? pricing.getCurrency() : "TZS";
    }

    /**
     * Checks if this session has expired
     */
    @Override
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Returns the number of payment attempts made
     */
    @Override
    public int getPaymentAttemptCount() {
        return paymentAttempts != null ? paymentAttempts.size() : 0;
    }

    /**
     * Checks if payment can be retried
     * Conditions:
     * - Status is PAYMENT_FAILED
     * - Session has not expired
     * - Less than 5 attempts made
     */
    @Override
    public boolean canRetryPayment() {
        return status == CheckoutSessionStatus.PAYMENT_FAILED
                && !isExpired()
                && getPaymentAttemptCount() < 5;
    }

    // ========================================
    // BUSINESS LOGIC METHODS
    // ========================================

    /**
     * Adds a payment attempt record to the session
     */
    public void addPaymentAttempt(PaymentAttempt attempt) {
        if (paymentAttempts == null) {
            paymentAttempts = new ArrayList<>();
        }
        attempt.setAttemptNumber(paymentAttempts.size() + 1);
        attempt.setAttemptedAt(LocalDateTime.now());
        paymentAttempts.add(attempt);
    }

    /**
     * Checks if this is an installment purchase
     */
    public boolean isInstallmentPurchase() {
        return CheckoutSessionType.INSTALLMENT.equals(sessionType);
    }

    /**
     * Checks if this is a group purchase
     */
    public boolean isGroupPurchase() {
        return CheckoutSessionType.GROUP_PURCHASE.equals(sessionType);
    }

    /**
     * Checks if this is a regular purchase (direct or cart)
     */
    public boolean isRegularPurchase() {
        return CheckoutSessionType.REGULAR_DIRECTLY.equals(sessionType)
                || CheckoutSessionType.REGULAR_CART.equals(sessionType);
    }

    /**
     * Marks the session as completed (payment successful)
     */
    public void markAsCompleted() {
        this.status = CheckoutSessionStatus.PAYMENT_COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks the session as failed with error message
     */
    public void markAsFailed(String errorMessage) {
        this.status = CheckoutSessionStatus.PAYMENT_FAILED;
        this.updatedAt = LocalDateTime.now();

        PaymentAttempt failedAttempt = PaymentAttempt.builder()
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();
        addPaymentAttempt(failedAttempt);
    }

    /**
     * Marks the session as expired
     */
    public void markAsExpired() {
        this.status = CheckoutSessionStatus.EXPIRED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Calculates the total quantity of all items
     */
    public int getTotalItemsQuantity() {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        return items.stream()
                .mapToInt(CheckoutItem::getQuantity)
                .sum();
    }

    /**
     * Gets the first item's shop ID (for single-shop checkouts)
     */
    public UUID getPrimaryShopId() {
        if (items == null || items.isEmpty()) {
            return null;
        }
        return items.get(0).getShopId();
    }

    /**
     * Checks if all items are from the same shop
     */
    public boolean isSingleShopCheckout() {
        if (items == null || items.size() <= 1) {
            return true;
        }
        UUID firstShopId = items.get(0).getShopId();
        return items.stream()
                .allMatch(item -> item.getShopId().equals(firstShopId));
    }

    // ========================================
    // NESTED CLASSES FOR JSON STORAGE
    // ========================================

    /**
     * Represents a single product item in the checkout
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutItem {

        /**
         * Product identifier
         */
        private UUID productId;

        /**
         * Shop/Store identifier
         */
        private UUID shopId;

        /**
         * Product name (snapshot at checkout time)
         */
        private String productName;

        private String productSlug;

        private String productImage;

        /**
         * Product SKU (Stock Keeping Unit)
         */
        private String productSku;

        /**
         * Quantity being purchased
         */
        private Integer quantity;

        /**
         * Unit price at checkout time
         */
        private BigDecimal unitPrice;

        /**
         * Subtotal for this item (quantity × unitPrice)
         */
        private BigDecimal subtotal;

        private BigDecimal tax;

        private BigDecimal total;

        private String shopName;

        private String shopLogo;

        private Boolean availableForCheckout;

        private Integer availableQuantity;

        /**
         * Product image URL
         */
        private String imageUrl;

        // ========================================
        // GROUP PURCHASE SPECIFIC FIELDS
        // ========================================

        /**
         * Group deal identifier (if applicable)
         */
        private UUID groupDealId;

        /**
         * Minimum participants required for group deal
         */
        private Integer minParticipants;

        /**
         * Current number of participants in group
         */
        private Integer currentParticipants;
    }

    /**
     * Comprehensive pricing breakdown
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricingDetails {

        /**
         * Sum of all item subtotals
         */
        private BigDecimal subtotal;

        /**
         * Shipping/delivery fee
         */
        private BigDecimal shippingFee;

        /**
         * Tax amount (if applicable)
         */
        private BigDecimal taxAmount;

        /**
         * Discount amount (if applied)
         */
        private BigDecimal discount;

        /**
         * Final total amount to be paid
         * Total = subtotal + shippingFee + taxAmount - discount
         */
        private BigDecimal total;

        /**
         * Currency code (e.g., "TZS", "USD")
         */
        @Builder.Default
        private String currency = "TZS";
    }

    /**
     * Payment provider information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentIntent {

        /**
         * Payment provider (e.g., "WALLET", "MPESA", "STRIPE")
         */
        private String provider;

        /**
         * Client secret for payment provider
         */
        private String clientSecret;

        /**
         * Available payment methods
         */
        private List<String> paymentMethods;

        /**
         * Payment intent status
         */
        private String status;
    }

    /**
     * Record of a single payment attempt
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentAttempt {

        /**
         * Attempt sequence number (1, 2, 3, ...)
         */
        private Integer attemptNumber;

        /**
         * Payment method used in this attempt
         */
        private String paymentMethod;

        /**
         * Status of this attempt (SUCCESS, FAILED, PENDING)
         */
        private String status;

        /**
         * Error message if failed
         */
        private String errorMessage;

        /**
         * When this attempt was made
         */
        private LocalDateTime attemptedAt;

        /**
         * External transaction ID (from payment provider)
         */
        private String transactionId;
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
        private Integer paymetnStartDelayDays;
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

    public void addCreatedOrderId(UUID orderId) {
        if (this.createdOrderIds == null) {
            this.createdOrderIds = new ArrayList<>();
        }
        this.createdOrderIds.add(orderId);
    }
}