package org.nextgate.nextgatebackend.financial_system.ledger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.financial_system.ledger.enums.LedgerEntryType;
import org.nextgate.nextgatebackend.wallet_service.escrow.utils.JsonMetadataConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries",
        indexes = {
                @Index(name = "idx_ledger_entry_number", columnList = "entryNumber"),
                @Index(name = "idx_ledger_entry_debit", columnList = "debit_account_id"),
                @Index(name = "idx_ledger_entry_credit", columnList = "credit_account_id"),
                @Index(name = "idx_ledger_entry_type", columnList = "entryType"),
                @Index(name = "idx_ledger_entry_reference", columnList = "referenceType, referenceId"),
                @Index(name = "idx_ledger_entry_created", columnList = "createdAt")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_entry_number", columnNames = "entryNumber")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String entryNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debit_account_id", nullable = false)
    private LedgerAccountEntity debitAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_account_id", nullable = false)
    private LedgerAccountEntity creditAccount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LedgerEntryType entryType;

    @Column(length = 50)
    private String referenceType;

    @Column(name = "referenceId")
    private UUID referenceId;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonMetadataConverter.class)
    private Map<String, Object> metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AccountEntity createdBy;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(length = 3)
    @Builder.Default
    private String currency = "TZS";

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public boolean isValid() {
        if (debitAccount == null || creditAccount == null) return false;
        if (debitAccount.getId().equals(creditAccount.getId())) return false;
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        if (entryType == null) return false;
        if (!debitAccount.getCurrency().equals(creditAccount.getCurrency())) return false;
        return true;
    }
}