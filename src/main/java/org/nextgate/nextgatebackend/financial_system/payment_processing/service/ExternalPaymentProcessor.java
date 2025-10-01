package org.nextgate.nextgatebackend.financial_system.payment_processing.service;

import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.financial_system.payment_processing.enums.PaymentMethod;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResult;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;

public interface ExternalPaymentProcessor {

    // Processes external payments (M-Pesa, cards, etc.)
    PaymentResult processPayment(
            CheckoutSessionEntity checkoutSession,
            PaymentMethod paymentMethod
    ) throws RandomExceptions;
}