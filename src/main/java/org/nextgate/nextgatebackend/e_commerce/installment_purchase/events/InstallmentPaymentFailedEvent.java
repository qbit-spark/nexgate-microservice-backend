
package org.nextgate.nextgatebackend.e_commerce.installment_purchase.events;

import lombok.Getter;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.entity.InstallmentPaymentEntity;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when an installment payment processing fails
 * due to insufficient funds, card issues, or technical problems.
 *
 * Triggers:
 * - Immediate failure notification to customer
 * - Retry scheduling
 * - Payment method update suggestions
 * - Grace period notifications
 */
@Getter
public class InstallmentPaymentFailedEvent extends ApplicationEvent {

    private final UUID paymentId;
    private final InstallmentPaymentEntity payment;
    private final LocalDateTime failedAt;
    private final String failureReason;
    private final String failureCode;
    private final int retryAttemptCount;
    private final boolean canRetry;
    private final LocalDateTime nextRetryAt;

    public InstallmentPaymentFailedEvent(
            Object source,
            UUID paymentId,
            InstallmentPaymentEntity payment,
            LocalDateTime failedAt,
            String failureReason,
            String failureCode,
            int retryAttemptCount,
            boolean canRetry,
            LocalDateTime nextRetryAt
    ) {
        super(source);
        this.paymentId = paymentId;
        this.payment = payment;
        this.failedAt = failedAt;
        this.failureReason = failureReason;
        this.failureCode = failureCode;
        this.retryAttemptCount = retryAttemptCount;
        this.canRetry = canRetry;
        this.nextRetryAt = nextRetryAt;
    }
}