package org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response for event checkout session
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventCheckoutResponse {

    private UUID sessionId;
    private CheckoutSessionStatus status;

    // Customer info
    private UUID customerId;
    private String customerUserName;

    // Event reference
    private UUID eventId;
    private String eventTitle;

    // Ticket details
    private TicketDetailsResponse ticketDetails;

    // Pricing
    private PricingSummaryResponse pricing;

    // Payment
    private PaymentIntentResponse paymentIntent;
    private List<PaymentAttemptResponse> paymentAttempts;

    // Ticket hold
    private Boolean ticketsHeld;
    private LocalDateTime ticketHoldExpiresAt;

    // Timestamps
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    // Booking reference (set after payment)
    private UUID createdBookingOrderId;

    // Helper flags
    private Boolean isExpired;
    private Boolean canRetryPayment;

    // ========================================
    // NESTED RESPONSE CLASSES
    // ========================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TicketDetailsResponse {
        private UUID ticketTypeId;
        private String ticketTypeName;
        private BigDecimal unitPrice;
        private Integer ticketsForBuyer;
        private List<OtherAttendeeResponse> otherAttendees;
        private Boolean sendTicketsToAttendees;
        private Integer totalQuantity;
        private BigDecimal subtotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OtherAttendeeResponse {
        private String name;
        private String email;
        private String phone;
        private Integer quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PricingSummaryResponse {
        private BigDecimal subtotal;
        private BigDecimal total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentIntentResponse {
        private String provider;
        private String clientSecret;
        private List<String> paymentMethods;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentAttemptResponse {
        private Integer attemptNumber;
        private String paymentMethod;
        private String status;
        private String errorMessage;
        private LocalDateTime attemptedAt;
        private String transactionId;
    }
}