package org.nextgate.nextgatebackend.checkout_session.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.checkout_session.payload.CheckoutSessionResponse;
import org.nextgate.nextgatebackend.checkout_session.payload.CreateCheckoutSessionRequest;
import org.nextgate.nextgatebackend.checkout_session.service.CheckoutSessionService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.web.bind.annotation.*;

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
}
