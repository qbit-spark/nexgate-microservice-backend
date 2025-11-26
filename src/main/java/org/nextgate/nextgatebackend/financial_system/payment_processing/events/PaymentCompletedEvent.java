package org.nextgate.nextgatebackend.financial_system.payment_processing.events;

import com.qbitspark.jikoexpress.financial_system.payment_processing.contract.PayableCheckoutSession;
import lombok.Getter;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class PaymentCompletedEvent extends ApplicationEvent {

    private final UUID checkoutSessionId;
    private final String sessionDomain; // "PRODUCT" or "EVENT"
    private final PayableCheckoutSession session;
    private final EscrowAccountEntity escrow;
    private final LocalDateTime completedAt;

    public PaymentCompletedEvent(
            Object source,
            UUID checkoutSessionId,
            String sessionDomain,
            PayableCheckoutSession session,
            EscrowAccountEntity escrow,
            LocalDateTime completedAt
    ) {
        super(source);
        this.checkoutSessionId = checkoutSessionId;
        this.sessionDomain = sessionDomain;
        this.session = session;
        this.escrow = escrow;
        this.completedAt = completedAt;
    }

    public boolean isProductDomain() {
        return "PRODUCT".equals(sessionDomain);
    }

    public boolean isEventDomain() {
        return "EVENT".equals(sessionDomain);
    }
}