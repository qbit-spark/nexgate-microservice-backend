package org.nextgate.nextgatebackend.payment_methods.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.nextgate.nextgatebackend.payment_methods.enums.PaymentMethodsType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentMethodSummaryResponse {

    private UUID paymentMethodId;
    private PaymentMethodsType paymentMethodType;
    private String displayName; // "Visa ****1234", "M-Pesa +254****5678"
    private Boolean isDefault;
    private Boolean isActive;
    private Boolean isVerified;
    private LocalDateTime createdAt;

    // Essential details for list display
    private SummaryDetails details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SummaryDetails {

        // For Cards
        private String cardType;
        private String lastFourDigits;

        // For PayPal/Email-based
        private String email;

        // For Mobile payments
        private String provider;

        // For Bank
        private String bankName;

        // For Crypto
        private String cryptoType;

        // For MNO (M-Pesa, Airtel, etc.)
        private String maskedPhoneNumber;

        // For Gift Cards
        private String currency;
        private Double balance;

        // Status indicator
        private String status; // "Active", "Inactive", "Pending"
    }
}