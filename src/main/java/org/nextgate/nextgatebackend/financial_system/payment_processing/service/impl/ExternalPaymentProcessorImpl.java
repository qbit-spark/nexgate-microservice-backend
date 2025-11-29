package org.nextgate.nextgatebackend.financial_system.payment_processing.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.financial_system.payment_processing.contract.PayableCheckoutSession;
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
            PayableCheckoutSession session,
            PaymentMethod paymentMethod) throws RandomExceptions {

        log.info("EXTERNAL PAYMENT PLACEHOLDER | Session: {} | Method: {}",
                session.getSessionId(), paymentMethod);

        throw new RandomExceptions(
                String.format("External payment method %s not yet implemented. Please use WALLET.",
                        paymentMethod)
        );
    }
}