package org.nextgate.nextgatebackend.payment_methods.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.nextgate.nextgatebackend.payment_methods.enums.PaymentMethodsType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentMethodDetailResponse {

    private UUID paymentMethodId;
    private UUID ownerId;
    private String ownerUserName;
    private PaymentMethodsType paymentMethodType;
    private PaymentMethodDetailsResponse methodDetails;
    private BillingAddressResponse billingAddress;
    private Map<String, Object> metadata;
    private Boolean isDefault;
    private Boolean isActive;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentMethodDetailsResponse {

        // Credit/Debit Card fields
        private String cardType;
        private String maskedCardNumber; // "**** **** **** 1234"
        private String expiry;
        private String cardholderName;

        // PayPal fields
        private String email;
        private String paypalId;

        // Bank Transfer fields
        private String bankName;
        private String maskedAccountNumber; // "****1234"
        private String routingNumber;
        private String accountType;
        private String accountHolderName;

        // Cryptocurrency fields
        private String cryptoType;
        private String maskedWalletAddress; // "1A1z...fNa"
        private String network;

        // Mobile Payment fields
        private String provider;
        private String deviceId;

        // Wallet fields
        private String walletType;
        private String walletId;

        // Gift Card fields
        private String maskedPin; // "****"
        private Double balance;
        private String currency;

        // Cash on Delivery fields
        private String instructions;

        // MNO Billing fields
        private String maskedPhoneNumber; // "+254****5678"
        private String mccMnc;

        private Map<String, Object> gatewayMetadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BillingAddressResponse {
        private String street;
        private String city;
        private String state;
        private String postalCode;
        private String country;
    }
}