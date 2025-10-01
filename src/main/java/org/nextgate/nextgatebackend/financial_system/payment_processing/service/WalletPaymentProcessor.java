package org.nextgate.nextgatebackend.financial_system.payment_processing.service;

import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResult;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;

public interface WalletPaymentProcessor {

    // Processes payment using buyer's wallet
    PaymentResult processPayment(CheckoutSessionEntity checkoutSession)
            throws ItemNotFoundException, RandomExceptions;
}