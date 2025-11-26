package org.nextgate.nextgatebackend.financial_system.payment_processing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAttempt {
    private Integer attemptNumber;
    private String paymentMethod;
    private String status;
    private String errorMessage;
    private LocalDateTime attemptedAt;
    private String transactionId;
}