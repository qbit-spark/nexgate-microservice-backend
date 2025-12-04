package org.nextgate.nextgatebackend.financial_system.payment_processing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntent {
    private String provider;
    private String clientSecret;
    private List<String> paymentMethods;
    private String status;
}