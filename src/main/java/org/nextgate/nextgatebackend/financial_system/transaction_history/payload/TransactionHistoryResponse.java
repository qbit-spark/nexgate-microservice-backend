package org.nextgate.nextgatebackend.financial_system.transaction_history.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionDirection;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionStatus;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionHistoryResponse {

    private UUID id;
    private String transactionRef;
    private TransactionType type;
    private TransactionDirection direction;
    private BigDecimal amount;
    private BigDecimal displayAmount;
    private String currency;
    private String title;
    private String description;
    private TransactionStatus status;
    private LocalDateTime createdAt;
    private String referenceType;
    private UUID referenceId;

    public static TransactionHistoryResponse fromEntity(
            org.nextgate.nextgatebackend.financial_system.transaction_history.entity.TransactionHistory entity) {

        return TransactionHistoryResponse.builder()
                .id(entity.getId())
                .transactionRef(entity.getTransactionRef())
                .type(entity.getType())
                .direction(entity.getDirection())
                .amount(entity.getAmount())
                .displayAmount(entity.getDisplayAmount())
                .currency(entity.getCurrency())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .build();
    }
}