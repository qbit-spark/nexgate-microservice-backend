package org.nextgate.nextgatebackend.payment_methods.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.payment_methods.payload.CreatePaymentMethodRequest;
import org.nextgate.nextgatebackend.payment_methods.payload.PaymentMethodDetailResponse;
import org.nextgate.nextgatebackend.payment_methods.payload.PaymentMethodListResponse;
import org.nextgate.nextgatebackend.payment_methods.service.PaymentMethodService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @PostMapping
    public GlobeSuccessResponseBuilder createPaymentMethod(@Valid @RequestBody CreatePaymentMethodRequest request)
            throws ItemNotFoundException, BadRequestException {
        PaymentMethodDetailResponse response = paymentMethodService.createPaymentMethod(request);
        return GlobeSuccessResponseBuilder.success("Payment method created successfully", response);
    }


    @GetMapping("/{paymentMethodId}")
    public GlobeSuccessResponseBuilder getPaymentMethodById(@PathVariable UUID paymentMethodId)
            throws ItemNotFoundException {
        PaymentMethodDetailResponse response = paymentMethodService.getPaymentMethodById(paymentMethodId);
        return GlobeSuccessResponseBuilder.success("Payment method retrieved successfully", response);
    }

    @GetMapping("/my-payment-methods")
    public GlobeSuccessResponseBuilder getMyPaymentMethods()
            throws ItemNotFoundException {
        PaymentMethodListResponse response = paymentMethodService.getMyPaymentMethods();
        return GlobeSuccessResponseBuilder.success("Payment methods retrieved successfully", response);
    }

    @PutMapping("/{paymentMethodId}")
    public GlobeSuccessResponseBuilder updatePaymentMethod(@PathVariable UUID paymentMethodId,
                                                           @Valid @RequestBody CreatePaymentMethodRequest request)
            throws ItemNotFoundException, BadRequestException {
        PaymentMethodDetailResponse response = paymentMethodService.updatePaymentMethod(paymentMethodId, request);
        return GlobeSuccessResponseBuilder.success("Payment method updated successfully", response);
    }

    @DeleteMapping("/{paymentMethodId}")
    public GlobeSuccessResponseBuilder deletePaymentMethod(@PathVariable UUID paymentMethodId)
            throws ItemNotFoundException {
        paymentMethodService.deletePaymentMethod(paymentMethodId);
        return GlobeSuccessResponseBuilder.success("Payment method deleted successfully", null);
    }

    @PatchMapping("/{paymentMethodId}/set-default")
    public GlobeSuccessResponseBuilder setAsDefault(@PathVariable UUID paymentMethodId)
            throws ItemNotFoundException, BadRequestException {
        PaymentMethodDetailResponse response = paymentMethodService.setAsDefault(paymentMethodId);
        return GlobeSuccessResponseBuilder.success("Payment method set as default successfully", response);
    }
}