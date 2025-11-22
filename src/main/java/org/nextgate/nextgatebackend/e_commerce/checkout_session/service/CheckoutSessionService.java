package org.nextgate.nextgatebackend.e_commerce.checkout_session.service;

import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.payload.CheckoutSessionResponse;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.payload.CheckoutSessionSummaryResponse;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.payload.UpdateCheckoutSessionRequest;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResponse;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.payload.CreateCheckoutSessionRequest;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;


import java.util.List;
import java.util.UUID;

public interface CheckoutSessionService {

    // Create new checkout session
    CheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request)
            throws ItemNotFoundException, BadRequestException;

    // Get checkout session by ID
    CheckoutSessionResponse getCheckoutSessionById(UUID sessionId)
            throws ItemNotFoundException;

    // Get all checkout sessions for authenticated user
    List<CheckoutSessionSummaryResponse> getMyCheckoutSessions()
            throws ItemNotFoundException;

//    // Cancel checkout session
    void cancelCheckoutSession(UUID sessionId)
            throws ItemNotFoundException, BadRequestException;

    CheckoutSessionResponse updateCheckoutSession(UUID sessionId, UpdateCheckoutSessionRequest request)
            throws ItemNotFoundException, BadRequestException;


    // Add this method to your existing CheckoutSessionService interface

    /**
     * Processes payment for a checkout session
     */
    PaymentResponse processPayment(UUID sessionId)
            throws ItemNotFoundException, BadRequestException, RandomExceptions;


    // Retry payment
    PaymentResponse retryPayment(UUID sessionId)
            throws ItemNotFoundException, BadRequestException, RandomExceptions;

    List<CheckoutSessionSummaryResponse> getMyActiveCheckoutSessions()
            throws ItemNotFoundException;

}