package org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.payload.CreateEventCheckoutRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.payload.EventCheckoutResponse;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.service.EventCheckoutService;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResponse;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/e-events/checkout")
@RequiredArgsConstructor
public class EventCheckoutController {

    private final EventCheckoutService checkoutService;

    /**
     * Create a new checkout session for event tickets
     */
    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createCheckoutSession(
            @Valid @RequestBody CreateEventCheckoutRequest request)
            throws ItemNotFoundException, RandomExceptions, BadRequestException {

        EventCheckoutResponse response = checkoutService.createCheckoutSession(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.CREATED)
                        .message("Checkout session created successfully")
                        .data(response)
                        .build());
    }

    /**
     * Get checkout session by ID
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getCheckoutSession(
            @PathVariable UUID sessionId)
            throws ItemNotFoundException {

        EventCheckoutResponse response = checkoutService.getCheckoutSessionById(sessionId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Checkout session retrieved successfully")
                        .data(response)
                        .build());
    }

    /**
     * Process payment for checkout session
     */
    @PostMapping("/{sessionId}/payment")
    public ResponseEntity<GlobeSuccessResponseBuilder> processPayment(
            @PathVariable UUID sessionId)
            throws ItemNotFoundException, RandomExceptions, BadRequestException {

        PaymentResponse response = checkoutService.processPayment(sessionId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Payment processed successfully")
                        .data(response)
                        .build());
    }

    /**
     * Cancel checkout session and release held tickets
     */
    @PostMapping("/{sessionId}/cancel")
    public ResponseEntity<GlobeSuccessResponseBuilder> cancelCheckoutSession(
            @PathVariable UUID sessionId)
            throws ItemNotFoundException, RandomExceptions, BadRequestException {

        checkoutService.cancelCheckoutSession(sessionId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Checkout session cancelled successfully")
                        .data(null)
                        .build());
    }

}