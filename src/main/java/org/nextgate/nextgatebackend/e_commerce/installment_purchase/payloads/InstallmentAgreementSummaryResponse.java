package org.nextgate.nextgatebackend.e_commerce.installment_purchase.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.AgreementStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentAgreementSummaryResponse {

    private UUID agreementId;
    private String agreementNumber;

    private UUID productId;
    private String productName;
    private String productImage;

    private UUID shopId;
    private String shopName;

    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal amountRemaining;
    private String currency;

    private Integer paymentsCompleted;
    private Integer paymentsRemaining;
    private Integer totalPayments;
    private Double progressPercentage;

    private LocalDateTime nextPaymentDate;
    private BigDecimal nextPaymentAmount;

    private AgreementStatus agreementStatus;
    private String agreementStatusDisplay;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    private Boolean canMakeEarlyPayment;
    private Boolean canCancel;
}