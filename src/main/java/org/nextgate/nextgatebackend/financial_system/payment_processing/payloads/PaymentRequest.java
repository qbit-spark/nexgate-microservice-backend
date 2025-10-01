package org.nextgate.nextgatebackend.financial_system.payment_processing.payloads;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.financial_system.payment_processing.enums.PaymentMethod;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentRequest {

    private UUID checkoutSessionId;

    // Optional: Override payment method from checkout session
    private PaymentMethod paymentMethod;

    // Optional: Idempotency key for duplicate prevention
    private String idempotencyKey;
}