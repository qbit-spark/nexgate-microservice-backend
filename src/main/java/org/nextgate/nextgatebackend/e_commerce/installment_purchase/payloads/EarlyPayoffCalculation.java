package org.nextgate.nextgatebackend.e_commerce.installment_purchase.payloads;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Data
@Builder
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