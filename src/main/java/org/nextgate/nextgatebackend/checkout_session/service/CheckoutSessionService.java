package org.nextgate.nextgatebackend.checkout_session.service;

import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.checkout_session.payload.CheckoutSessionResponse;
import org.nextgate.nextgatebackend.checkout_session.payload.CheckoutSessionSummaryResponse;
import org.nextgate.nextgatebackend.checkout_session.payload.UpdateCheckoutSessionRequest;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.checkout_session.payload.CreateCheckoutSessionRequest;


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



//    // Retry payment
    CheckoutSessionResponse retryPayment(UUID sessionId)
            throws ItemNotFoundException, BadRequestException;

    List<CheckoutSessionSummaryResponse> getMyActiveCheckoutSessions()
            throws ItemNotFoundException;

}