package org.nextgate.nextgatebackend.financial_system.payment_processing.payloads;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.financial_system.payment_processing.enums.PaymentMethod;
import org.nextgate.nextgatebackend.financial_system.payment_processing.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {

    private Boolean success;

    private PaymentStatus status;

    private String message;

    // Checkout session details
    private UUID checkoutSessionId;

    // Escrow details
    private UUID escrowId;
    private String escrowNumber;

    // Order details (null if not created yet)
    private UUID orderId;
    private String orderNumber;

    // Payment details
    private PaymentMethod paymentMethod;
    private BigDecimal amountPaid;
    private BigDecimal platformFee;
    private BigDecimal sellerAmount;
    private String currency;

    // For external payments (M-Pesa, etc.)
    private String paymentUrl;        // URL to complete payment
    private String ussdCode;          // USSD code for mobile money
    private String referenceNumber;   // External payment reference
}