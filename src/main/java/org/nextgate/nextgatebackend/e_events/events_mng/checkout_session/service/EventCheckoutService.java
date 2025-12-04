package org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.service;


import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.payload.CreateEventCheckoutRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.payload.EventCheckoutResponse;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResponse;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;

import java.util.UUID;

public interface EventCheckoutService {

    EventCheckoutResponse createCheckoutSession(CreateEventCheckoutRequest request)
            throws ItemNotFoundException, BadRequestException;

    EventCheckoutResponse getCheckoutSessionById(UUID sessionId)
            throws ItemNotFoundException;

    PaymentResponse processPayment(UUID sessionId)
            throws ItemNotFoundException, BadRequestException, RandomExceptions;

    void cancelCheckoutSession(UUID sessionId)
            throws ItemNotFoundException, BadRequestException;
}