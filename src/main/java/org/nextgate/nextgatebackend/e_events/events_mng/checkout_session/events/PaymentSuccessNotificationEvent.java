package org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.events;

import lombok.Getter;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class PaymentSuccessNotificationEvent extends ApplicationEvent {

    private final UUID sessionId;
    private final AccountEntity customer;
    private final EscrowAccountEntity escrow;

    public PaymentSuccessNotificationEvent(
            Object source,
            UUID sessionId,
            AccountEntity customer,
            EscrowAccountEntity escrow) {

        super(source);
        this.sessionId = sessionId;
        this.customer = customer;
        this.escrow = escrow;
    }
}