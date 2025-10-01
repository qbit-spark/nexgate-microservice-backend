package org.nextgate.nextgatebackend.financial_system.payment_processing.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.financial_system.payment_processing.enums.PaymentStatus;

import java.util.UUID;

// Internal result passed between payment processors and orchestrator
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResult {

    private PaymentStatus status;

    private String message;

    // Escrow created during payment
    private EscrowAccountEntity escrow;

    // Order created after payment (null if not created)
    private UUID orderId;

    // For external payments
    private String externalReference;
    private String paymentUrl;
    private String ussdCode;

    // Error details if failed
    private String errorCode;
    private String errorMessage;

    // Helper methods
    public boolean isSuccess() {
        return PaymentStatus.SUCCESS.equals(status);
    }

    public boolean isFailed() {
        return PaymentStatus.FAILED.equals(status);
    }

    public boolean isPending() {
        return PaymentStatus.PENDING.equals(status);
    }
}