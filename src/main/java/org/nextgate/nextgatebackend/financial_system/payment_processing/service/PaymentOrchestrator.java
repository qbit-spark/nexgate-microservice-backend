package org.nextgate.nextgatebackend.financial_system.payment_processing.service;

import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentRequest;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResponse;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;

import java.util.UUID;

public interface PaymentOrchestrator {

    // Main entry point for payment processing
    PaymentResponse processPayment(UUID checkoutSessionId)
            throws ItemNotFoundException, RandomExceptions;

    // Overload with payment request
    PaymentResponse processPayment(PaymentRequest request)
            throws ItemNotFoundException, RandomExceptions;
}