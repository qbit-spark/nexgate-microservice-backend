package org.nextgate.nextgatebackend.payment_methods.utils.validators;

import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.payment_methods.entity.PaymentMethodsEntity;
import org.nextgate.nextgatebackend.payment_methods.payload.CreatePaymentMethodRequest;
import org.nextgate.nextgatebackend.payment_methods.repo.PaymentMethodRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentMethodValidator {

    private final PaymentMethodRepository paymentMethodRepository;

    public void validatePaymentMethodDetails(CreatePaymentMethodRequest request) throws BadRequestException {
        switch (request.getPaymentMethodType()) {
            case CREDIT_CARD, DEBIT_CARD -> validateCardDetails(request);
            case PAYPAL -> validatePayPalDetails(request);
            case BANK_TRANSFER -> validateBankTransferDetails(request);
            case MNO_PAYMENT -> validateMnoDetails(request);
            case CRYPTOCURRENCY -> validateCryptoDetails(request);
            case GIFT_CARD -> validateGiftCardDetails(request);
            case MOBILE_PAYMENT -> validateMobilePaymentDetails(request);
            case CASH_ON_DELIVERY -> validateCashOnDeliveryDetails(request);
        }
    }

    public void checkForDuplicatePaymentMethod(AccountEntity account, CreatePaymentMethodRequest request) throws BadRequestException {
        // Get all payment methods of the same type for this user
        List<PaymentMethodsEntity> existingMethods = paymentMethodRepository
                .findByOwnerAndPaymentMethodType(account, request.getPaymentMethodType());

        boolean exists = switch (request.getPaymentMethodType()) {
            case CREDIT_CARD, DEBIT_CARD -> checkDuplicateCard(existingMethods, request);
            case PAYPAL -> checkDuplicatePayPal(existingMethods, request);
            case MNO_PAYMENT -> checkDuplicateMno(existingMethods, request);
            case BANK_TRANSFER -> checkDuplicateBank(existingMethods, request);
            case CRYPTOCURRENCY -> checkDuplicateCrypto(existingMethods, request);
            case MOBILE_PAYMENT -> checkDuplicateMobilePayment(existingMethods, request);
            default -> false; // GIFT_CARD and CASH_ON_DELIVERY can have duplicates
        };

        if (exists) {
            throw new BadRequestException("This payment method already exists for your account");
        }
    }

    // Card validation
    private void validateCardDetails(CreatePaymentMethodRequest request) throws BadRequestException {
        var details = request.getMethodDetails();
        if (details.getCardNumber() == null || details.getCardNumber().trim().isEmpty()) {
            throw new BadRequestException("Card number is required");
        }
        if (details.getExpiry() == null || details.getExpiry().trim().isEmpty()) {
            throw new BadRequestException("Expiry date is required");
        }
        if (details.getCardholderName() == null || details.getCardholderName().trim().isEmpty()) {
            throw new BadRequestException("Cardholder name is required");
        }
    }

    // PayPal validation
    private void validatePayPalDetails(CreatePaymentMethodRequest request) throws BadRequestException {
        var details = request.getMethodDetails();
        if (details.getEmail() == null || details.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Email is required for PayPal");
        }
    }

    // Bank transfer validation
    private void validateBankTransferDetails(CreatePaymentMethodRequest request) throws BadRequestException {
        var details = request.getMethodDetails();
        if (details.getBankName() == null || details.getBankName().trim().isEmpty()) {
            throw new BadRequestException("Bank name is required");
        }
        if (details.getAccountNumber() == null || details.getAccountNumber().trim().isEmpty()) {
            throw new BadRequestException("Account number is required");
        }
        if (details.getAccountHolderName() == null || details.getAccountHolderName().trim().isEmpty()) {
            throw new BadRequestException("Account holder name is required");
        }
    }

    // MNO (Mobile Network Operator) validation
    private void validateMnoDetails(CreatePaymentMethodRequest request) throws BadRequestException {
        var details = request.getMethodDetails();
        if (details.getPhoneNumber() == null || details.getPhoneNumber().trim().isEmpty()) {
            throw new BadRequestException("Phone number is required for mobile money");
        }
    }

    // Cryptocurrency validation
    private void validateCryptoDetails(CreatePaymentMethodRequest request) throws BadRequestException {
        var details = request.getMethodDetails();
        if (details.getWalletAddress() == null || details.getWalletAddress().trim().isEmpty()) {
            throw new BadRequestException("Wallet address is required");
        }
        if (details.getCryptoType() == null || details.getCryptoType().trim().isEmpty()) {
            throw new BadRequestException("Cryptocurrency type is required");
        }
    }

    // Gift card validation
    private void validateGiftCardDetails(CreatePaymentMethodRequest request) throws BadRequestException {
        var details = request.getMethodDetails();
        if (details.getPin() == null || details.getPin().trim().isEmpty()) {
            throw new BadRequestException("PIN is required for gift cards");
        }
        if (details.getBalance() == null || details.getBalance() <= 0) {
            throw new BadRequestException("Valid balance is required for gift cards");
        }
    }

    // Mobile payment validation
    private void validateMobilePaymentDetails(CreatePaymentMethodRequest request) throws BadRequestException {
        var details = request.getMethodDetails();
        if (details.getProvider() == null || details.getProvider().trim().isEmpty()) {
            throw new BadRequestException("Provider is required for mobile payments");
        }
    }


    // Cash on delivery validation (minimal)
    private void validateCashOnDeliveryDetails(CreatePaymentMethodRequest request) {
        // COD typically doesn't need much validation
    }

    // Duplicate checking methods - now check in-memory
    private boolean checkDuplicateCard(List<PaymentMethodsEntity> existingMethods, CreatePaymentMethodRequest request) {
        String cardNumber = request.getMethodDetails().getCardNumber();
        if (cardNumber == null || cardNumber.length() < 4) return false;

        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return existingMethods.stream()
                .anyMatch(method -> {
                    var details = method.getMethodDetails();
                    return details != null && details.getCardNumber() != null
                            && details.getCardNumber().endsWith(lastFour);
                });
    }

    private boolean checkDuplicatePayPal(List<PaymentMethodsEntity> existingMethods, CreatePaymentMethodRequest request) {
        String email = request.getMethodDetails().getEmail();
        if (email == null) return false;

        return existingMethods.stream()
                .anyMatch(method -> {
                    var details = method.getMethodDetails();
                    return details != null && email.equals(details.getEmail());
                });
    }

    private boolean checkDuplicateMno(List<PaymentMethodsEntity> existingMethods, CreatePaymentMethodRequest request) {
        String phoneNumber = request.getMethodDetails().getPhoneNumber();
        if (phoneNumber == null) return false;

        return existingMethods.stream()
                .anyMatch(method -> {
                    var details = method.getMethodDetails();
                    return details != null && phoneNumber.equals(details.getPhoneNumber());
                });
    }

    private boolean checkDuplicateBank(List<PaymentMethodsEntity> existingMethods, CreatePaymentMethodRequest request) {
        String accountNumber = request.getMethodDetails().getAccountNumber();
        if (accountNumber == null) return false;

        return existingMethods.stream()
                .anyMatch(method -> {
                    var details = method.getMethodDetails();
                    return details != null && accountNumber.equals(details.getAccountNumber());
                });
    }

    private boolean checkDuplicateCrypto(List<PaymentMethodsEntity> existingMethods, CreatePaymentMethodRequest request) {
        String walletAddress = request.getMethodDetails().getWalletAddress();
        if (walletAddress == null) return false;

        return existingMethods.stream()
                .anyMatch(method -> {
                    var details = method.getMethodDetails();
                    return details != null && walletAddress.equals(details.getWalletAddress());
                });
    }

    private boolean checkDuplicateMobilePayment(List<PaymentMethodsEntity> existingMethods, CreatePaymentMethodRequest request) {
        String provider = request.getMethodDetails().getProvider();
        if (provider == null) return false;

        return existingMethods.stream()
                .anyMatch(method -> {
                    var details = method.getMethodDetails();
                    return details != null && provider.equals(details.getProvider());
                });
    }


}