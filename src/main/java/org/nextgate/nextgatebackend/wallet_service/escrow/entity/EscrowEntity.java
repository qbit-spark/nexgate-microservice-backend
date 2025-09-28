package org.nextgate.nextgatebackend.wallet_service.escrow.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.wallet_service.escrow.enums.EscrowStatus;
import org.nextgate.nextgatebackend.wallet_service.escrow.utils.JsonMetadataConverter;
import org.nextgate.nextgatebackend.wallet_service.transactions.entity.TransactionEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "escrow",
        indexes = {
                @Index(name = "idx_escrow_order", columnList = "orderId"),
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
public class EscrowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private AccountEntity buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private AccountEntity seller;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private EscrowStatus status = EscrowStatus.HELD;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_transaction_id", nullable = false)
    private TransactionEntity sourceTransaction;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonMetadataConverter.class)
    private Map<String, Object> metadata;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}