package org.nextgate.nextgatebackend.financial_system.payment_processing.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.financial_system.payment_processing.enums.PaymentMethod;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResult;
import org.nextgate.nextgatebackend.financial_system.payment_processing.service.ExternalPaymentProcessor;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ExternalPaymentProcessorImpl implements ExternalPaymentProcessor {

    @Override
    public PaymentResult processPayment(
            CheckoutSessionEntity checkoutSession,
            PaymentMethod paymentMethod) throws RandomExceptions {

        log.info("EXTERNAL PAYMENT PLACEHOLDER: Checkout session: {}, Payment method: {}",
                checkoutSession.getSessionId(), paymentMethod);

        // TODO: Implement external payment integration in Phase 8
        throw new RandomExceptions(
                String.format("External payment method %s not yet implemented. Please use WALLET payment.",
                        paymentMethod)
        );
    }
}