package org.nextgate.nextgatebackend.financial_system.ledger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.financial_system.ledger.enums.LedgerAccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_accounts",
        indexes = {
                @Index(name = "idx_ledger_account_type", columnList = "accountType"),
                @Index(name = "idx_ledger_account_owner", columnList = "owner_id"),
                @Index(name = "idx_ledger_account_number", columnList = "accountNumber"),
                @Index(name = "idx_ledger_account_active", columnList = "isActive")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_account_number", columnNames = "accountNumber")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LedgerAccountType accountType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AccountEntity owner;

    @Column(name = "escrow_reference_id")
    private UUID escrowReferenceId;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(length = 3)
    @Builder.Default
    private String currency = "TZS";

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic methods
    public boolean canDebit(BigDecimal amount) {
        if (!isActive) return false;
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        return currentBalance.compareTo(amount) >= 0;
    }

    public boolean isUserWallet() {
        return LedgerAccountType.USER_WALLET.equals(accountType);
    }

    public boolean isEscrow() {
        return LedgerAccountType.ESCROW.equals(accountType);
    }

    public boolean isPlatformAccount() {
        return LedgerAccountType.PLATFORM_REVENUE.equals(accountType) ||
                LedgerAccountType.PLATFORM_RESERVE.equals(accountType);
    }
}