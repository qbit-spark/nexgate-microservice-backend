package org.nextgate.nextgatebackend.financial_system.payment_processing.callbacks;

import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResult;

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
    );

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