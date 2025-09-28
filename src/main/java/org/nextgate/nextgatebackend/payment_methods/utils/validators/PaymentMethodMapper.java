package org.nextgate.nextgatebackend.payment_methods.utils.validators;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.payment_methods.entity.PaymentMethodsEntity;
import org.nextgate.nextgatebackend.payment_methods.payload.CreatePaymentMethodRequest;
import org.nextgate.nextgatebackend.payment_methods.payload.PaymentMethodDetailResponse;
import org.nextgate.nextgatebackend.payment_methods.payload.PaymentMethodSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PaymentMethodMapper {

    public PaymentMethodsEntity toEntity(CreatePaymentMethodRequest request, AccountEntity owner) {
        return PaymentMethodsEntity.builder()
                .owner(owner)
                .paymentMethodType(request.getPaymentMethodType())
                .methodDetails(mapToEntityDetails(request.getMethodDetails()))
                .billingAddress(mapToEntityBillingAddress(request.getBillingAddress()))
                .metadata(request.getMetadata())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .build();
    }

    public PaymentMethodDetailResponse toDetailResponse(PaymentMethodsEntity entity) {
        return PaymentMethodDetailResponse.builder()
                .paymentMethodId(entity.getPaymentMethodId())
                .ownerId(entity.getOwner().getAccountId())
                .ownerUserName(entity.getOwner().getUserName())
                .paymentMethodType(entity.getPaymentMethodType())
                .methodDetails(mapToResponseDetails(entity.getMethodDetails(), entity.getPaymentMethodType()))
                .billingAddress(mapToResponseBillingAddress(entity.getBillingAddress()))
                .metadata(entity.getMetadata())
                .isDefault(entity.getIsDefault())
                .isActive(entity.getIsActive())
                .isVerified(entity.getIsVerified())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PaymentMethodSummaryResponse toSummaryResponse(PaymentMethodsEntity entity) {
        return PaymentMethodSummaryResponse.builder()
                .paymentMethodId(entity.getPaymentMethodId())
                .paymentMethodType(entity.getPaymentMethodType())
                .displayName(generateDisplayName(entity))
                .isDefault(entity.getIsDefault())
                .isActive(entity.getIsActive())
                .isVerified(entity.getIsVerified())
                .createdAt(entity.getCreatedAt())
                .details(mapToSummaryDetails(entity))
                .build();
    }

    public List<PaymentMethodSummaryResponse> toSummaryResponseList(List<PaymentMethodsEntity> entities) {
        return entities.stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }

    // Private mapping methods
    private PaymentMethodsEntity.PaymentMethodDetails mapToEntityDetails(CreatePaymentMethodRequest.PaymentMethodDetailsDto dto) {
        if (dto == null) return null;

        return PaymentMethodsEntity.PaymentMethodDetails.builder()
                .cardType(dto.getCardType())
                .cardNumber(dto.getCardNumber()) // Should be tokenized in real implementation
                .expiry(dto.getExpiry())
                .cardholderName(dto.getCardholderName())
                .email(dto.getEmail())
                .paypalId(dto.getPaypalId())
                .bankName(dto.getBankName())
                .accountNumber(dto.getAccountNumber()) // Should be tokenized
                .routingNumber(dto.getRoutingNumber())
                .accountType(dto.getAccountType())
                .accountHolderName(dto.getAccountHolderName())
                .cryptoType(dto.getCryptoType())
                .walletAddress(dto.getWalletAddress())
                .network(dto.getNetwork())
                .provider(dto.getProvider())
                .deviceId(dto.getDeviceId())
                .walletType(dto.getWalletType())
                .walletId(dto.getWalletId())
                .pin(dto.getPin()) // Should be tokenized
                .balance(dto.getBalance())
                .currency(dto.getCurrency())
                .instructions(dto.getInstructions())
                .phoneNumber(dto.getPhoneNumber())
                .mccMnc(dto.getMccMnc())
                .gatewayMetadata(dto.getGatewayMetadata())
                .build();
    }

    private PaymentMethodsEntity.BillingAddress mapToEntityBillingAddress(CreatePaymentMethodRequest.BillingAddressDto dto) {
        if (dto == null) return null;

        return PaymentMethodsEntity.BillingAddress.builder()
                .street(dto.getStreet())
                .city(dto.getCity())
                .state(dto.getState())
                .postalCode(dto.getPostalCode())
                .country(dto.getCountry())
                .build();
    }

    private PaymentMethodDetailResponse.PaymentMethodDetailsResponse mapToResponseDetails(
            PaymentMethodsEntity.PaymentMethodDetails details,
            org.nextgate.nextgatebackend.payment_methods.enums.PaymentMethodsType type) {
        if (details == null) return null;

        return PaymentMethodDetailResponse.PaymentMethodDetailsResponse.builder()
                .cardType(details.getCardType())
                .maskedCardNumber(maskCardNumber(details.getCardNumber()))
                .expiry(details.getExpiry())
                .cardholderName(details.getCardholderName())
                .email(details.getEmail())
                .paypalId(details.getPaypalId())
                .bankName(details.getBankName())
                .maskedAccountNumber(maskAccountNumber(details.getAccountNumber()))
                .routingNumber(details.getRoutingNumber())
                .accountType(details.getAccountType())
                .accountHolderName(details.getAccountHolderName())
                .cryptoType(details.getCryptoType())
                .maskedWalletAddress(maskWalletAddress(details.getWalletAddress()))
                .network(details.getNetwork())
                .provider(details.getProvider())
                .deviceId(details.getDeviceId())
                .walletType(details.getWalletType())
                .walletId(details.getWalletId())
                .maskedPin(maskPin(details.getPin()))
                .balance(details.getBalance())
                .currency(details.getCurrency())
                .instructions(details.getInstructions())
                .maskedPhoneNumber(maskPhoneNumber(details.getPhoneNumber()))
                .mccMnc(details.getMccMnc())
                .gatewayMetadata(details.getGatewayMetadata())
                .build();
    }

    private PaymentMethodDetailResponse.BillingAddressResponse mapToResponseBillingAddress(PaymentMethodsEntity.BillingAddress address) {
        if (address == null) return null;

        return PaymentMethodDetailResponse.BillingAddressResponse.builder()
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .build();
    }

    private PaymentMethodSummaryResponse.SummaryDetails mapToSummaryDetails(PaymentMethodsEntity entity) {
        var details = entity.getMethodDetails();
        if (details == null) return null;

        return PaymentMethodSummaryResponse.SummaryDetails.builder()
                .cardType(details.getCardType())
                .lastFourDigits(getLastFourDigits(details.getCardNumber()))
                .email(details.getEmail())
                .provider(details.getProvider())
                .bankName(details.getBankName())
                .cryptoType(details.getCryptoType())
                .maskedPhoneNumber(maskPhoneNumber(details.getPhoneNumber()))
                .currency(details.getCurrency())
                .balance(details.getBalance())
                .status(getStatusString(entity))
                .build();
    }

    private String generateDisplayName(PaymentMethodsEntity entity) {
        var details = entity.getMethodDetails();
        if (details == null) return entity.getPaymentMethodType().toString();

        return switch (entity.getPaymentMethodType()) {
            case CREDIT_CARD, DEBIT_CARD ->
                    String.format("%s ****%s", details.getCardType(), getLastFourDigits(details.getCardNumber()));
            case PAYPAL ->
                    String.format("PayPal (%s)", details.getEmail());
            case BANK_TRANSFER ->
                    String.format("%s ****%s", details.getBankName(), getLastFourDigits(details.getAccountNumber()));
            case MNO_PAYMENT ->
                    String.format("M-Pesa %s", maskPhoneNumber(details.getPhoneNumber()));
            case MOBILE_PAYMENT ->
                    String.format("%s", details.getProvider());
            case WALLET ->
                    String.format("%s", details.getWalletType());
            case CRYPTOCURRENCY ->
                    String.format("%s", details.getCryptoType());
            case GIFT_CARD ->
                    String.format("Gift Card (%s)", details.getCurrency());
            case CASH_ON_DELIVERY ->
                    "Cash on Delivery";
        };
    }

    // Masking utility methods
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return "****";
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    private String maskWalletAddress(String walletAddress) {
        if (walletAddress == null || walletAddress.length() < 8) return "****";
        return walletAddress.substring(0, 4) + "..." + walletAddress.substring(walletAddress.length() - 4);
    }

    private String maskPin(String pin) {
        return pin != null ? "****" : null;
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) return "****";
        return phoneNumber.substring(0, 4) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }

    private String getLastFourDigits(String number) {
        if (number == null || number.length() < 4) return "****";
        return number.substring(number.length() - 4);
    }

    private String getStatusString(PaymentMethodsEntity entity) {
        if (!entity.getIsActive()) return "Inactive";
        if (!entity.getIsVerified()) return "Pending";
        return "Active";
    }
}