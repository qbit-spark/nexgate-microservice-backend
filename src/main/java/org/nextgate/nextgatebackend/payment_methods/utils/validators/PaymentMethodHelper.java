package org.nextgate.nextgatebackend.payment_methods.utils.validators;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.payment_methods.entity.PaymentMethodsEntity;
import org.nextgate.nextgatebackend.payment_methods.repo.PaymentMethodRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentMethodHelper {

    private final PaymentMethodRepository paymentMethodRepository;

    public void unsetCurrentDefaultPaymentMethod(AccountEntity account) {
        List<PaymentMethodsEntity> defaultMethods = paymentMethodRepository.findByOwnerAndIsDefaultTrue(account);
        for (PaymentMethodsEntity method : defaultMethods) {
            method.setIsDefault(false);
        }
        if (!defaultMethods.isEmpty()) {
            paymentMethodRepository.saveAll(defaultMethods);
        }
    }
}