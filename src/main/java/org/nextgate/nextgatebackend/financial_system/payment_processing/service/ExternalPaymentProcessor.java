package org.nextgate.nextgatebackend.financial_system.payment_processing.service;

import org.nextgate.nextgatebackend.financial_system.payment_processing.contract.PayableCheckoutSession;
import org.nextgate.nextgatebackend.financial_system.payment_processing.enums.PaymentMethod;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResult;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;


public interface ExternalPaymentProcessor {

    PaymentResult processPayment(
            PayableCheckoutSession session,
            PaymentMethod paymentMethod
    ) throws RandomExceptions;
}