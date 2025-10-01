package org.nextgate.nextgatebackend.financial_system.wallet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WalletEntity - User wallet metadata only
 * Actual balance is stored in LedgerAccountEntity
 * This entity just tracks wallet status and links to user
 */
@Entity
@Table(name = "wallets",
        indexes = {
                @Index(name = "idx_wallet_account", columnList = "account_id"),
                @Index(name = "idx_wallet_is_active", columnList = "isActive"),
                @Index(name = "idx_wallet_created_at", columnList = "createdAt")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wallet_account", columnNames = "account_id")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // One-to-one relationship with user account
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    // Wallet status - can be deactivated for security
    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    // Timestamps
    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Optional: Track last activity
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    // Optional: Reason for deactivation
    @Column(name = "deactivation_reason", length = 500)
    private String deactivationReason;

    // Optional: Who deactivated (admin reference)
    @Column(name = "deactivated_by")
    private UUID deactivatedBy;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic method
    public void activate() {
        this.isActive = true;
        this.deactivationReason = null;
        this.deactivatedBy = null;
        this.deactivatedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic method
    public void deactivate(UUID deactivatedBy, String reason) {
        this.isActive = false;
        this.deactivatedBy = deactivatedBy;
        this.deactivationReason = reason;
        this.deactivatedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Track activity
    public void recordActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }
}