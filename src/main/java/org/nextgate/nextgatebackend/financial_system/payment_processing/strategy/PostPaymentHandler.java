package org.nextgate.nextgatebackend.financial_system.payment_processing.strategy;

import org.nextgate.nextgatebackend.financial_system.payment_processing.contract.PayableCheckoutSession;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.globe_enums.CheckoutSessionsDomains;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;

public interface PostPaymentHandler {

    void handlePostPayment(
            PayableCheckoutSession session,
            EscrowAccountEntity escrow
    ) throws BadRequestException, ItemNotFoundException;

    CheckoutSessionsDomains getSupportedDomain(); // ← Enum

    default boolean canHandle(PayableCheckoutSession session) {
        return getSupportedDomain() == session.getSessionDomain();
    }
}