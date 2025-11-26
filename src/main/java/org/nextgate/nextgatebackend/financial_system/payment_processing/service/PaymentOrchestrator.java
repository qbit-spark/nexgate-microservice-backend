package org.nextgate.nextgatebackend.financial_system.payment_processing.service;

import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentRequest;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResponse;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;


import java.util.UUID;

public interface PaymentOrchestrator {

    PaymentResponse processPayment(UUID checkoutSessionId, String sessionDomain)
            throws ItemNotFoundException, RandomExceptions, BadRequestException;

    PaymentResponse processPayment(PaymentRequest request)
            throws ItemNotFoundException, RandomExceptions, BadRequestException;
}