package org.nextgate.nextgatebackend.e_commerce.checkout_session.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.payload.CheckoutSessionResponse;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.payload.CheckoutSessionSummaryResponse;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.payload.CreateCheckoutSessionRequest;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.payload.UpdateCheckoutSessionRequest;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.service.ProductsCheckoutSessionService;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResponse;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/checkout-sessions")
@RequiredArgsConstructor
public class CheckoutSessionController {

    private final ProductsCheckoutSessionService productsCheckoutSessionService;

    @PostMapping
    public GlobeSuccessResponseBuilder createCheckoutSession(
            @Valid @RequestBody CreateCheckoutSessionRequest request)
            throws ItemNotFoundException, BadRequestException {

        CheckoutSessionResponse response = productsCheckoutSessionService.createCheckoutSession(request);

        return GlobeSuccessResponseBuilder.success(
                "Checkout session created successfully",
                response
        );
    }

    @GetMapping("/{sessionId}")
    public GlobeSuccessResponseBuilder getCheckoutSessionById(@PathVariable UUID sessionId)
            throws ItemNotFoundException, BadRequestException {
        CheckoutSessionResponse response = productsCheckoutSessionService.getCheckoutSessionById(sessionId);
        return GlobeSuccessResponseBuilder.success("Checkout session retrieved successfully", response);
    }

    @GetMapping
    public GlobeSuccessResponseBuilder getMyCheckoutSessions() throws ItemNotFoundException {
        List<CheckoutSessionSummaryResponse> responses = productsCheckoutSessionService.getMyCheckoutSessions();
        return GlobeSuccessResponseBuilder.success("Checkout sessions retrieved successfully", responses);
    }

    @DeleteMapping("/{sessionId}/cancel")
    public GlobeSuccessResponseBuilder cancelCheckoutSession(
            @PathVariable UUID sessionId)
            throws ItemNotFoundException, BadRequestException {

        productsCheckoutSessionService.cancelCheckoutSession(sessionId);

        return GlobeSuccessResponseBuilder.success(
                "Checkout session cancelled successfully",
                null
        );
    }

    @PatchMapping("/{sessionId}")
    public GlobeSuccessResponseBuilder updateCheckoutSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody UpdateCheckoutSessionRequest request)
            throws ItemNotFoundException, BadRequestException {

        CheckoutSessionResponse response = productsCheckoutSessionService.updateCheckoutSession(sessionId, request);

        return GlobeSuccessResponseBuilder.success(
                "Checkout session updated successfully",
                response
        );
    }


    @PostMapping("/{sessionId}/process-payment")
    public GlobeSuccessResponseBuilder processPayment(@PathVariable UUID sessionId)
            throws ItemNotFoundException, BadRequestException, RandomExceptions {

        PaymentResponse response = productsCheckoutSessionService.processPayment(sessionId);

        return GlobeSuccessResponseBuilder.success(
                response.getSuccess() ? "Payment processed successfully" : "Payment processing initiated",
                response
        );
    }


    @PostMapping("/{sessionId}/retry-payment")
    public GlobeSuccessResponseBuilder retryPayment(@PathVariable UUID sessionId)
            throws ItemNotFoundException, BadRequestException, RandomExceptions {

        PaymentResponse response = productsCheckoutSessionService.retryPayment(sessionId);

        return GlobeSuccessResponseBuilder.success(
                response.getSuccess() ? "Payment retry successful" : "Payment retry failed",
                response
        );
    }

    @GetMapping("/active")
    public GlobeSuccessResponseBuilder getMyActiveCheckoutSessions()
            throws ItemNotFoundException {

        List<CheckoutSessionSummaryResponse> responses =
                productsCheckoutSessionService.getMyActiveCheckoutSessions();

        return GlobeSuccessResponseBuilder.success(
                "Active checkout sessions retrieved successfully",
                responses
        );
    }
}
