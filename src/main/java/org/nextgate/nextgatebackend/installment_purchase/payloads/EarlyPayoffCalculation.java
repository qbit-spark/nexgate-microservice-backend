package org.nextgate.nextgatebackend.installment_purchase.payloads;


import lombok.Data;
import org.nextgate.nextgatebackend.checkout_session.payload.CheckoutSessionResponse;
import org.nextgate.nextgatebackend.installment_purchase.enums.AgreementStatus;
import org.nextgate.nextgatebackend.installment_purchase.enums.FulfillmentTiming;
import org.nextgate.nextgatebackend.installment_purchase.enums.PaymentFrequency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Data
public class EarlyPayoffCalculation {

    private UUID agreementId;

    // Current state
    private Integer paymentsCompleted;
    private Integer paymentsRemaining;
    private BigDecimal amountPaid;

    // Payoff amounts
    private BigDecimal remainingPrincipal;
    private BigDecimal unaccruedInterest; // Interest not yet charged
    private BigDecimal interestRebate; // If applying rebate

    // Options
    private BigDecimal payoffWithRebate;
    private BigDecimal payoffWithoutRebate;

    // Savings
    private BigDecimal savingsVsScheduled;

    // Info
    private String rebatePolicy;
    private LocalDateTime calculatedAt;
}