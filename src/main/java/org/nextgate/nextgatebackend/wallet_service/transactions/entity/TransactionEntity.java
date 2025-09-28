package org.nextgate.nextgatebackend.wallet_service.transactions.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.wallet_service.escrow.utils.JsonMetadataConverter;
import org.nextgate.nextgatebackend.wallet_service.transactions.enums.TransactionStatus;
import org.nextgate.nextgatebackend.wallet_service.transactions.enums.TransactionType;
import org.nextgate.nextgatebackend.wallet_service.wallet.entity.WalletEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "transactions",
        indexes = {
                @Index(name = "idx_transaction_ref", columnList = "transactionRef"),
                @Index(name = "idx_transaction_account", columnList = "account_id"),
                @Index(name = "idx_transaction_wallet", columnList = "wallet_id"),
                @Index(name = "idx_transaction_status", columnList = "status"),
                @Index(name = "idx_transaction_source", columnList = "transactionSource"),
                @Index(name = "idx_transaction_type", columnList = "transactionType"),
                @Index(name = "idx_transaction_created", columnList = "createdAt"),
                @Index(name = "idx_transaction_account_created", columnList = "account_id, createdAt")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false, length = 10)
    private String transactionRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private WalletEntity wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;


    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "related_transaction_id")
    private String relatedTransactionId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "escrow_release_date")
    private LocalDateTime escrowReleaseDate;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonMetadataConverter.class)
    private Map<String, Object> metadata;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}