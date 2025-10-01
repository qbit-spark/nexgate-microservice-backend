package org.nextgate.nextgatebackend.financial_system.transaction_history.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionDirection;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionStatus;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction_history",
        indexes = {
                @Index(name = "idx_txn_history_account", columnList = "account_id"),
                @Index(name = "idx_txn_history_type", columnList = "type"),
                @Index(name = "idx_txn_history_direction", columnList = "direction"),
                @Index(name = "idx_txn_history_status", columnList = "status"),
                @Index(name = "idx_txn_history_created", columnList = "createdAt"),
                @Index(name = "idx_txn_history_ref", columnList = "transactionRef")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false, length = 20)
    private String transactionRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionDirection direction;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "TZS";

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "ledger_entry_id")
    private UUID ledgerEntryId;

    @Column(length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.COMPLETED;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public BigDecimal getDisplayAmount() {
        return direction == TransactionDirection.DEBIT ? amount.negate() : amount;
    }

    public boolean isCredit() {
        return TransactionDirection.CREDIT.equals(direction);
    }

    public boolean isDebit() {
        return TransactionDirection.DEBIT.equals(direction);
    }
}