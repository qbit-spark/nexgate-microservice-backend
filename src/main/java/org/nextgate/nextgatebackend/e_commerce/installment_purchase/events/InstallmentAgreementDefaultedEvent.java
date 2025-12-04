package org.nextgate.nextgatebackend.e_commerce.installment_purchase.events;

import lombok.Getter;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.entity.InstallmentAgreementEntity;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when an installment agreement is marked as DEFAULTED
 * due to missed payments beyond grace period.
 *
 * Triggers:
 * - Customer default notification
 * - Admin alert for collections
 * - Credit reporting updates
 * - Legal process initiation
 */
@Getter
public class InstallmentAgreementDefaultedEvent extends ApplicationEvent {

    private final UUID agreementId;
    private final InstallmentAgreementEntity agreement;
    private final LocalDateTime defaultedAt;
    private final String defaultReason;
    private final BigDecimal totalOverdueAmount;
    private final int daysOverdue;
    private final int missedPaymentCount;

    public InstallmentAgreementDefaultedEvent(
            Object source,
            UUID agreementId,
            InstallmentAgreementEntity agreement,
            LocalDateTime defaultedAt,
            String defaultReason,
            BigDecimal totalOverdueAmount,
            int daysOverdue,
            int missedPaymentCount
    ) {
        super(source);
        this.agreementId = agreementId;
        this.agreement = agreement;
        this.defaultedAt = defaultedAt;
        this.defaultReason = defaultReason;
        this.totalOverdueAmount = totalOverdueAmount;
        this.daysOverdue = daysOverdue;
        this.missedPaymentCount = missedPaymentCount;
    }
}