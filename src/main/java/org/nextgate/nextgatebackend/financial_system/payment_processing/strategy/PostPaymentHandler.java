package org.nextgate.nextgatebackend.financial_system.payment_processing.strategy;

import com.qbitspark.jikoexpress.financial_system.payment_processing.contract.PayableCheckoutSession;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;

public interface PostPaymentHandler {

    void handlePostPayment(
            PayableCheckoutSession session,
            EscrowAccountEntity escrow
    ) throws BadRequestException, ItemNotFoundException;

    String getSupportedDomain();

    default boolean canHandle(PayableCheckoutSession session) {
        return getSupportedDomain().equals(session.getSessionDomain());
    }
}