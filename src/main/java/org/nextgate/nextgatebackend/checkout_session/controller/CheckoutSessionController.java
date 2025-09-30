package org.nextgate.nextgatebackend.checkout_session.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.checkout_session.payload.CheckoutSessionResponse;
import org.nextgate.nextgatebackend.checkout_session.payload.CheckoutSessionSummaryResponse;
import org.nextgate.nextgatebackend.checkout_session.payload.CreateCheckoutSessionRequest;
import org.nextgate.nextgatebackend.checkout_session.service.CheckoutSessionService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/checkout-sessions")
@RequiredArgsConstructor
public class CheckoutSessionController {

    private final CheckoutSessionService checkoutSessionService;

    @PostMapping
    public GlobeSuccessResponseBuilder createCheckoutSession(
            @Valid @RequestBody CreateCheckoutSessionRequest request)
            throws ItemNotFoundException, BadRequestException {

        CheckoutSessionResponse response = checkoutSessionService.createCheckoutSession(request);

        return GlobeSuccessResponseBuilder.success(
                "Checkout session created successfully",
                response
        );
    }

    @GetMapping("/{sessionId}")
    public GlobeSuccessResponseBuilder getCheckoutSessionById(@PathVariable UUID sessionId)
            throws ItemNotFoundException, BadRequestException {
        CheckoutSessionResponse response = checkoutSessionService.getCheckoutSessionById(sessionId);
        return GlobeSuccessResponseBuilder.success("Checkout session retrieved successfully", response);
    }

    @GetMapping
    public GlobeSuccessResponseBuilder getMyCheckoutSessions() throws ItemNotFoundException {
        List<CheckoutSessionSummaryResponse> responses = checkoutSessionService.getMyCheckoutSessions();
        return GlobeSuccessResponseBuilder.success("Checkout sessions retrieved successfully", responses);
    }

}
