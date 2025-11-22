package org.nextgate.nextgatebackend.e_commerce.installment_purchase.events;

import lombok.Getter;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.entity.InstallmentAgreementEntity;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a customer pays off their entire installment agreement
 * before the scheduled completion date.
 *
 * Triggers:
 * - Congratulatory notification to customer
 * - Interest savings summary
 * - Early completion certificate
 * - Analytics tracking for customer behavior
 */
@Getter
public class InstallmentEarlyPayoffEvent extends ApplicationEvent {

    private final UUID agreementId;
    private final InstallmentAgreementEntity agreement;
    private final LocalDateTime payoffAt;
    private final BigDecimal payoffAmount;
    private final BigDecimal interestSaved;
    private final BigDecimal totalSavings;
    private final int paymentsSkipped;
    private final LocalDateTime originalCompletionDate;

    public InstallmentEarlyPayoffEvent(
            Object source,
            UUID agreementId,
            InstallmentAgreementEntity agreement,
            LocalDateTime payoffAt,
            BigDecimal payoffAmount,
            BigDecimal interestSaved,
            BigDecimal totalSavings,
            int paymentsSkipped,
            LocalDateTime originalCompletionDate
    ) {
        super(source);
        this.agreementId = agreementId;
        this.agreement = agreement;
        this.payoffAt = payoffAt;
        this.payoffAmount = payoffAmount;
        this.interestSaved = interestSaved;
        this.totalSavings = totalSavings;
        this.paymentsSkipped = paymentsSkipped;
        this.originalCompletionDate = originalCompletionDate;
    }
}