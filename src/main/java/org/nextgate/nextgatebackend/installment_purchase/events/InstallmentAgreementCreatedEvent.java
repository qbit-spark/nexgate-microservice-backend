package org.nextgate.nextgatebackend.installment_purchase.events;

import lombok.Getter;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentAgreementEntity;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class InstallmentAgreementCreatedEvent extends ApplicationEvent {

    private final UUID agreementId;
    private final UUID checkoutSessionId;
    private final InstallmentAgreementEntity agreement;
    private final boolean requiresImmediateOrder;
    private final LocalDateTime createdAt;

    public InstallmentAgreementCreatedEvent(
            Object source,
            UUID agreementId,
            UUID checkoutSessionId,
            InstallmentAgreementEntity agreement,
            boolean requiresImmediateOrder
    ) {
        super(source);
        this.agreementId = agreementId;
        this.checkoutSessionId = checkoutSessionId;
        this.agreement = agreement;
        this.requiresImmediateOrder = requiresImmediateOrder;
        this.createdAt = LocalDateTime.now();
    }
}