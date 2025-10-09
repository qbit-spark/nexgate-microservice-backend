package org.nextgate.nextgatebackend.installment_purchase.events;

import lombok.Getter;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentAgreementEntity;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when an installment agreement is fully paid
 * and marked as COMPLETED.
 *
 * Triggers:
 * - Order creation (for AFTER_PAYMENT fulfillment)
 * - Notification to customer
 * - Analytics tracking
 */
@Getter
public class InstallmentAgreementCompletedEvent extends ApplicationEvent {

    private final UUID agreementId;
    private final InstallmentAgreementEntity agreement;
    private final LocalDateTime completedAt;
    private final boolean isEarlyPayoff;

    public InstallmentAgreementCompletedEvent(
            Object source,
            UUID agreementId,
            InstallmentAgreementEntity agreement,
            LocalDateTime completedAt,
            boolean isEarlyPayoff
    ) {
        super(source);
        this.agreementId = agreementId;
        this.agreement = agreement;
        this.completedAt = completedAt;
        this.isEarlyPayoff = isEarlyPayoff;
    }
}