package org.nextgate.nextgatebackend.financial_system.escrow.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.enums.EscrowStatus;
import org.nextgate.nextgatebackend.financial_system.escrow.utils.JsonMetadataConverter;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "escrow_accounts",
        indexes = {
                @Index(name = "idx_escrow_session", columnList = "checkout_session_id, session_domain"),
                @Index(name = "idx_escrow_buyer", columnList = "buyer_id"),
                @Index(name = "idx_escrow_seller", columnList = "seller_id"),
                @Index(name = "idx_escrow_status", columnList = "status"),
                @Index(name = "idx_escrow_created", columnList = "createdAt")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscrowAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String escrowNumber;

    // Universal session reference
    @Column(name = "checkout_session_id", nullable = false)
    private UUID checkoutSessionId;

    @Column(name = "session_domain", nullable = false, length = 20)
    private String sessionDomain; // "PRODUCT" or "EVENT"

    @Column(name = "order_id")
    private String orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private AccountEntity buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private AccountEntity seller;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal platformFeePercentage = BigDecimal.valueOf(5.0);

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal platformFeeAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal sellerAmount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "TZS";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EscrowStatus status = EscrowStatus.HELD;

    @Column(name = "ledger_account_id")
    private UUID ledgerAccountId;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonMetadataConverter.class)
    private Map<String, Object> metadata;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "disputed_at")
    private LocalDateTime disputedAt;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void calculateFees() {
        this.platformFeeAmount = totalAmount
                .multiply(platformFeePercentage)
                .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);

        this.sellerAmount = totalAmount.subtract(platformFeeAmount);
    }

    public void markAsReleased() {
        this.status = EscrowStatus.RELEASED;
        this.releasedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsRefunded() {
        this.status = EscrowStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsDisputed() {
        this.status = EscrowStatus.DISPUTED;
        this.disputedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean canRelease() {
        return status == EscrowStatus.HELD;
    }

    public boolean canRefund() {
        return status == EscrowStatus.HELD || status == EscrowStatus.DISPUTED;
    }

    public boolean isHeld() {
        return status == EscrowStatus.HELD;
    }

    public boolean isReleased() {
        return status == EscrowStatus.RELEASED;
    }

    public boolean isRefunded() {
        return status == EscrowStatus.REFUNDED;
    }
}