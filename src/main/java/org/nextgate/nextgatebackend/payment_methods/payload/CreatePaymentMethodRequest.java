package org.nextgate.nextgatebackend.payment_methods.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.nextgate.nextgatebackend.payment_methods.enums.PaymentMethodsType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreatePaymentMethodRequest {

    @NotNull(message = "Payment method type is required")
    private PaymentMethodsType paymentMethodType;

    @Valid
    @NotNull(message = "Payment method details are required")
    private PaymentMethodDetailsDto methodDetails;

    @Valid
    private BillingAddressDto billingAddress;

    private Map<String, Object> metadata;

    private Boolean isDefault = false;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentMethodDetailsDto {

        // Credit/Debit Card fields
        private String cardType;

        @Pattern(regexp = "^[0-9]{13,19}$", message = "Invalid card number")
        private String cardNumber;

        @Pattern(regexp = "^(0[1-9]|1[0-2])/[0-9]{2,4}$", message = "Invalid expiry format (MM/YY)")
        private String expiry;

        @Size(min = 2, max = 100, message = "Cardholder name required")
        private String cardholderName;

        // PayPal fields
        @Email(message = "Invalid email")
        private String email;
        private String paypalId;

        // Bank Transfer fields
        @Size(min = 2, max = 100, message = "Bank name required")
        private String bankName;
        private String accountNumber;
        private String routingNumber;
        private String accountType;

        @Size(min = 2, max = 100, message = "Account holder name required")
        private String accountHolderName;

        // Cryptocurrency fields
        private String cryptoType;
        private String walletAddress;
        private String network;

        // Mobile Payment fields
        private String provider;
        private String deviceId;

        // Wallet fields
        private String walletType;
        private String walletId;

        // Gift Card fields
        private String pin;

        @DecimalMin(value = "0.01", message = "Balance must be greater than 0")
        private Double balance;

        @Pattern(regexp = "^[A-Z]{3}$", message = "Invalid currency code")
        private String currency;

        // Cash on Delivery fields
        private String instructions;

        // MNO Billing fields
        @Pattern(regexp = "^\\+[1-9]\\d{8,14}$", message = "Invalid phone number")
        private String phoneNumber;
        private String mccMnc;

        private Map<String, Object> gatewayMetadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BillingAddressDto {

        @NotBlank(message = "Street is required")
        private String street;

        @NotBlank(message = "City is required")
        private String city;

        private String state;
        private String postalCode;

        @NotBlank(message = "Country is required")
        private String country;
    }
}