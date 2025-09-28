package org.nextgate.nextgatebackend.payment_methods.utils.validators;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.payment_methods.entity.PaymentMethodsEntity;
import org.nextgate.nextgatebackend.payment_methods.payload.CreatePaymentMethodRequest;
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

    public void updatePaymentMethodFields(PaymentMethodsEntity existingEntity, CreatePaymentMethodRequest request) {
        // Update payment method type if provided
        if (request.getPaymentMethodType() != null) {
            existingEntity.setPaymentMethodType(request.getPaymentMethodType());
        }

        // Update method details (partial update)
        if (request.getMethodDetails() != null) {
            updateMethodDetails(existingEntity, request.getMethodDetails());
        }

        // Update billing address (partial update)
        if (request.getBillingAddress() != null) {
            updateBillingAddress(existingEntity, request.getBillingAddress());
        }

        // Update metadata if provided
        if (request.getMetadata() != null) {
            existingEntity.setMetadata(request.getMetadata());
        }

        // Update isDefault if provided
        if (request.getIsDefault() != null) {
            existingEntity.setIsDefault(request.getIsDefault());
        }
    }

    private void updateMethodDetails(PaymentMethodsEntity existingEntity, CreatePaymentMethodRequest.PaymentMethodDetailsDto newDetails) {
        PaymentMethodsEntity.PaymentMethodDetails existingDetails = existingEntity.getMethodDetails();

        // If no existing details, create new ones
        if (existingDetails == null) {
            existingDetails = PaymentMethodsEntity.PaymentMethodDetails.builder().build();
        }

        // Update only provided fields - keep existing values for null fields
        if (newDetails.getCardType() != null) existingDetails.setCardType(newDetails.getCardType());
        if (newDetails.getCardNumber() != null) existingDetails.setCardNumber(newDetails.getCardNumber());
        if (newDetails.getExpiry() != null) existingDetails.setExpiry(newDetails.getExpiry());
        if (newDetails.getCardholderName() != null) existingDetails.setCardholderName(newDetails.getCardholderName());

        if (newDetails.getEmail() != null) existingDetails.setEmail(newDetails.getEmail());
        if (newDetails.getPaypalId() != null) existingDetails.setPaypalId(newDetails.getPaypalId());

        if (newDetails.getBankName() != null) existingDetails.setBankName(newDetails.getBankName());
        if (newDetails.getAccountNumber() != null) existingDetails.setAccountNumber(newDetails.getAccountNumber());
        if (newDetails.getRoutingNumber() != null) existingDetails.setRoutingNumber(newDetails.getRoutingNumber());
        if (newDetails.getAccountType() != null) existingDetails.setAccountType(newDetails.getAccountType());
        if (newDetails.getAccountHolderName() != null) existingDetails.setAccountHolderName(newDetails.getAccountHolderName());

        if (newDetails.getCryptoType() != null) existingDetails.setCryptoType(newDetails.getCryptoType());
        if (newDetails.getWalletAddress() != null) existingDetails.setWalletAddress(newDetails.getWalletAddress());
        if (newDetails.getNetwork() != null) existingDetails.setNetwork(newDetails.getNetwork());

        if (newDetails.getProvider() != null) existingDetails.setProvider(newDetails.getProvider());
        if (newDetails.getDeviceId() != null) existingDetails.setDeviceId(newDetails.getDeviceId());

        if (newDetails.getWalletType() != null) existingDetails.setWalletType(newDetails.getWalletType());
        if (newDetails.getWalletId() != null) existingDetails.setWalletId(newDetails.getWalletId());

        if (newDetails.getPin() != null) existingDetails.setPin(newDetails.getPin());
        if (newDetails.getBalance() != null) existingDetails.setBalance(newDetails.getBalance());
        if (newDetails.getCurrency() != null) existingDetails.setCurrency(newDetails.getCurrency());

        if (newDetails.getInstructions() != null) existingDetails.setInstructions(newDetails.getInstructions());

        if (newDetails.getPhoneNumber() != null) existingDetails.setPhoneNumber(newDetails.getPhoneNumber());
        if (newDetails.getMccMnc() != null) existingDetails.setMccMnc(newDetails.getMccMnc());

        if (newDetails.getGatewayMetadata() != null) existingDetails.setGatewayMetadata(newDetails.getGatewayMetadata());

        // Set the updated details back to the entity
        existingEntity.setMethodDetails(existingDetails);
    }

    private void updateBillingAddress(PaymentMethodsEntity existingEntity, CreatePaymentMethodRequest.BillingAddressDto newAddress) {
        PaymentMethodsEntity.BillingAddress existingAddress = existingEntity.getBillingAddress();

        // If no existing address, create new one
        if (existingAddress == null) {
            existingAddress = PaymentMethodsEntity.BillingAddress.builder().build();
        }

        // Update only provided fields - keep existing values for null fields
        if (newAddress.getStreet() != null) existingAddress.setStreet(newAddress.getStreet());
        if (newAddress.getCity() != null) existingAddress.setCity(newAddress.getCity());
        if (newAddress.getState() != null) existingAddress.setState(newAddress.getState());
        if (newAddress.getPostalCode() != null) existingAddress.setPostalCode(newAddress.getPostalCode());
        if (newAddress.getCountry() != null) existingAddress.setCountry(newAddress.getCountry());

        // Set the updated address back to the entity
        existingEntity.setBillingAddress(existingAddress);
    }
}