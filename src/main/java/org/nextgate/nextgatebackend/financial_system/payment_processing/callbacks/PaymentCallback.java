package org.nextgate.nextgatebackend.financial_system.payment_processing.callbacks;

import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResult;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;

/**
 * Callback interface for payment processing events
 */
public interface PaymentCallback {

    /**
     * Called after successful payment and escrow creation
     */
    void onPaymentSuccess(
            CheckoutSessionEntity checkoutSession,
            EscrowAccountEntity escrow
    ) throws BadRequestException, ItemNotFoundException;

    /**
     * Called when payment fails
     */
    void onPaymentFailure(
            CheckoutSessionEntity checkoutSession,
            PaymentResult result,
            String errorMessage
    );

    /**
     * Called when payment is pending (for external payments)
     */
    void onPaymentPending(
            CheckoutSessionEntity checkoutSession,
            PaymentResult result
    );
}