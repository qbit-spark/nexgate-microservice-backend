package org.nextgate.nextgatebackend.financial_system.payment_processing.callbacks;

import com.qbitspark.jikoexpress.financial_system.payment_processing.contract.PayableCheckoutSession;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResult;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;

/**
 * Callback interface for payment processing events
 */
public interface PaymentCallback {

    void onPaymentSuccess(
            PayableCheckoutSession session,
            EscrowAccountEntity escrow
    ) throws BadRequestException, ItemNotFoundException, RandomExceptions;

    void onPaymentFailure(
            PayableCheckoutSession session,
            PaymentResult result,
            String errorMessage
    );

    void onPaymentPending(
            PayableCheckoutSession session,
            PaymentResult result
    );
}