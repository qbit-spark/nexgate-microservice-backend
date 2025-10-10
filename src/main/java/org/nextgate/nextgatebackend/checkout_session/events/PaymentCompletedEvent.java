package org.nextgate.nextgatebackend.checkout_session.events;

import lombok.Getter;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when payment is completed and confirmed.
 *
 * This event is published for ALL payment types:
 * - Instant payments (wallet)
 * - Webhook-based payments (Stripe, PayPal, M-Pesa)
 *
 * Triggers:
 * - Order creation (for applicable session types)
 * - Notification to customer
 * - Analytics tracking
 */
@Getter
public class PaymentCompletedEvent extends ApplicationEvent {

    private final UUID checkoutSessionId;
    private final CheckoutSessionEntity session;
    private final String transactionId;
    private final LocalDateTime completedAt;

    public PaymentCompletedEvent(
            Object source,
            UUID checkoutSessionId,
            CheckoutSessionEntity session,
            String transactionId,
            LocalDateTime completedAt
    ) {
        super(source);
        this.checkoutSessionId = checkoutSessionId;
        this.session = session;
        this.transactionId = transactionId;
        this.completedAt = completedAt;
    }
}