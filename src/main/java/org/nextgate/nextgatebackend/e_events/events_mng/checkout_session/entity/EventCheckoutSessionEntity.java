package org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.entity;


import com.qbitspark.jikoexpress.financial_system.payment_processing.contract.PayableCheckoutSession;
import jakarta.persistence.*;
import lombok.*;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.utils.TicketCheckoutDetailsJsonConverter;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.financial_system.payment_processing.utils.PaymentAttemptsJsonConverter;
import org.nextgate.nextgatebackend.financial_system.payment_processing.utils.PaymentIntentJsonConverter;
import org.nextgate.nextgatebackend.financial_system.payment_processing.utils.PricingDetailsJsonConverter;
import org.nextgate.nextgatebackend.globe_enums.CheckoutSessionsDomains;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "event_checkout_sessions", indexes = {
        @Index(name = "idx_event_checkout_customer", columnList = "customer_id"),
        @Index(name = "idx_event_checkout_status", columnList = "status"),
        @Index(name = "idx_event_checkout_event", columnList = "event_id"),
        @Index(name = "idx_event_checkout_expires", columnList = "expires_at"),
        @Index(name = "idx_event_checkout_escrow", columnList = "escrow_id")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventCheckoutSessionEntity implements PayableCheckoutSession {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID sessionId;

    // ========================================
    // PAYMENT PARTICIPANT (PAYER)
    // ========================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id", nullable = false)
    private AccountEntity customer;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;


    // ========================================
    // SESSION STATUS
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CheckoutSessionStatus status = CheckoutSessionStatus.PENDING_PAYMENT;

    // ========================================
    // TICKET CHECKOUT DETAILS
    // ========================================

    @Column(name = "ticket_details", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = TicketCheckoutDetailsJsonConverter.class)
    private TicketCheckoutDetails ticketDetails;

    // ========================================
    // PRICING SUMMARY
    // ========================================

    @Column(name = "pricing", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = PricingDetailsJsonConverter.class)
    private PricingSummary pricing;

    // ========================================
    // PAYMENT INTENT
    // ========================================

    @Column(name = "payment_intent", columnDefinition = "jsonb")
    @Convert(converter = PaymentIntentJsonConverter.class)
    private PaymentIntent paymentIntent;

    // ========================================
    // PAYMENT ATTEMPTS TRACKING
    // ========================================

    @Column(name = "payment_attempts", columnDefinition = "jsonb")
    @Convert(converter = PaymentAttemptsJsonConverter.class)
    @Builder.Default
    private List<PaymentAttempt> paymentAttempts = new ArrayList<>();

    // ========================================
    // TICKET HOLD INFORMATION
    // ========================================

    @Column(name = "tickets_held")
    @Builder.Default
    private Boolean ticketsHeld = false;

    @Column(name = "ticket_hold_expires_at")
    private LocalDateTime ticketHoldExpiresAt;

    // ========================================
    // SESSION TIMING
    // ========================================

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // ========================================
    // PAYMENT RESULT REFERENCES
    // ========================================

    @Column(name = "escrow_id")
    private UUID escrowId;

    @Column(name = "created_booking_order_id")
    private UUID createdBookingOrderId;

    // ========================================
    // LIFECYCLE CALLBACKS
    // ========================================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMinutes(15);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========================================
    // PAYMENT CONTRACT IMPLEMENTATION
    // ========================================

    @Override
    public AccountEntity getPayer() {
        return customer;
    }

    @Override
    public CheckoutSessionsDomains getSessionDomain() {
        return CheckoutSessionsDomains.EVENT;
    }

    @Override
    public BigDecimal getTotalAmount() {
        return pricing != null ? pricing.getTotal() : BigDecimal.ZERO;
    }

    @Override
    public String getCurrency() {
        return pricing != null ? pricing.getCurrency() : "TZS";
    }

    @Override
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    @Override
    public int getPaymentAttemptCount() {
        return paymentAttempts != null ? paymentAttempts.size() : 0;
    }

    @Override
    public boolean canRetryPayment() {
        return status == CheckoutSessionStatus.PAYMENT_FAILED
                && !isExpired()
                && getPaymentAttemptCount() < 5;
    }

    // ========================================
    // BUSINESS LOGIC METHODS
    // ========================================

    public void addPaymentAttempt(PaymentAttempt attempt) {
        if (paymentAttempts == null) {
            paymentAttempts = new ArrayList<>();
        }
        attempt.setAttemptNumber(paymentAttempts.size() + 1);
        attempt.setAttemptedAt(LocalDateTime.now());
        paymentAttempts.add(attempt);
    }

    public void markAsCompleted() {
        this.status = CheckoutSessionStatus.PAYMENT_COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = CheckoutSessionStatus.PAYMENT_FAILED;
        this.updatedAt = LocalDateTime.now();

        PaymentAttempt failedAttempt = PaymentAttempt.builder()
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();
        addPaymentAttempt(failedAttempt);
    }

    public void markAsExpired() {
        this.status = CheckoutSessionStatus.EXPIRED;
        this.updatedAt = LocalDateTime.now();
    }

    // ========================================
    // NESTED CLASSES FOR JSON STORAGE
    // ========================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketCheckoutDetails {
        private UUID ticketTypeId;
        private String ticketTypeName;
        private BigDecimal unitPrice;
        private Integer ticketsForBuyer;
        private List<OtherAttendee> otherAttendees;
        private Boolean sendTicketsToAttendees;
        private Integer totalQuantity;
        private BigDecimal subtotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtherAttendee {
        private String name;
        private String email;
        private String phone;
        private Integer quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricingSummary {
        private BigDecimal subtotal;
        private BigDecimal total;
        @Builder.Default
        private String currency = "TZS";
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
}