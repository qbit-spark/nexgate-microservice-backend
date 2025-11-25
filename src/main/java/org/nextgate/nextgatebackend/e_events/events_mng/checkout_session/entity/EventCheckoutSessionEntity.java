package org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.entity;


import jakarta.persistence.*;
import lombok.*;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.utils.PaymentAttemptsJsonConverter;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.utils.PaymentIntentJsonConverter;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.utils.PricingSummaryJsonConverter;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.utils.TicketCheckoutDetailsJsonConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing an event ticket checkout session
 * Holds ticket selections and attendee information while awaiting payment
 */
@Entity
@Table(name = "event_checkout_sessions", indexes = {
        @Index(name = "idx_event_checkout_customer", columnList = "customer_id"),
        @Index(name = "idx_event_checkout_status", columnList = "status"),
        @Index(name = "idx_event_checkout_event", columnList = "event_id"),
        @Index(name = "idx_event_checkout_expires", columnList = "expires_at")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventCheckoutSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID sessionId;

    // ========================================
    // CUSTOMER & EVENT REFERENCES
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
    private CheckoutSessionStatus status;

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
    @Convert(converter = PricingSummaryJsonConverter.class)
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
    private List<PaymentAttempt> paymentAttempts = new ArrayList<>();

    // ========================================
    // TICKET HOLD INFORMATION
    // ========================================

    @Column(name = "tickets_held")
    private Boolean ticketsHeld = false;

    @Column(name = "ticket_hold_expires_at")
    private LocalDateTime ticketHoldExpiresAt;

    // ========================================
    // SESSION TIMING
    // ========================================

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // ========================================
    // CREATED BOOKING ORDER REFERENCE
    // ========================================

    @Column(name = "created_booking_order_id")
    private UUID createdBookingOrderId;

    // ========================================
    // LIFECYCLE CALLBACKS
    // ========================================

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

    // ========================================
    // BUSINESS LOGIC METHODS
    // ========================================

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

    // ========================================
    // NESTED CLASSES FOR JSON STORAGE
    // ========================================

    /**
     * Represents ticket checkout details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketCheckoutDetails {
        private UUID ticketTypeId;
        private String ticketTypeName;
        private BigDecimal unitPrice;
        private Integer ticketsForBuyer;  // Number of tickets for buyer
        private List<OtherAttendee> otherAttendees;  // Other attendees with quantities
        private Boolean sendTicketsToAttendees;  // QR distribution preference
        private Integer totalQuantity;  // Total tickets (buyer + others)
        private BigDecimal subtotal;
    }

    /**
     * Other attendee information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtherAttendee {
        private String name;
        private String email;
        private String phone;
        private Integer quantity;  // Number of tickets for this person
    }

    /**
     * Pricing summary for the checkout
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricingSummary {
        private BigDecimal subtotal;
        private BigDecimal total;
        private String currency;
    }

    /**
     * Payment intent details
     */
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

    /**
     * Payment attempt record
     */
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