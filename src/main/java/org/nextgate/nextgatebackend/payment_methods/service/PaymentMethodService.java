package org.nextgate.nextgatebackend.payment_methods.service;

import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.payment_methods.payload.CreatePaymentMethodRequest;
import org.nextgate.nextgatebackend.payment_methods.payload.PaymentMethodDetailResponse;
import org.nextgate.nextgatebackend.payment_methods.payload.PaymentMethodListResponse;

import java.util.UUID;

public interface PaymentMethodService {

    PaymentMethodDetailResponse createPaymentMethod(CreatePaymentMethodRequest request) throws ItemNotFoundException, BadRequestException;

    PaymentMethodDetailResponse getPaymentMethodById(UUID paymentMethodId);

    PaymentMethodListResponse getMyPaymentMethods();

    PaymentMethodDetailResponse updatePaymentMethod(UUID paymentMethodId, CreatePaymentMethodRequest request);

    void deletePaymentMethod(UUID paymentMethodId);

    PaymentMethodDetailResponse setAsDefault(UUID paymentMethodId);

}