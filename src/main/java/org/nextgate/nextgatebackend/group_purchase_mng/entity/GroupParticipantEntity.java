package org.nextgate.nextgatebackend.group_purchase_mng.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.enums.ParticipantStatus;
import org.nextgate.nextgatebackend.group_purchase_mng.utils.PurchaseHistoryJsonConverter;
import org.nextgate.nextgatebackend.group_purchase_mng.utils.TransferHistoryJsonConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "group_participants", indexes = {
        @Index(name = "idx_participant_group", columnList = "group_instance_id"),
        @Index(name = "idx_participant_user", columnList = "user_id"),
        @Index(name = "idx_participant_checkout", columnList = "checkout_session_id"),
        @Index(name = "idx_participant_status", columnList = "status")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID participantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_instance_id", referencedColumnName = "groupInstanceId", nullable = false)
    private GroupPurchaseInstanceEntity groupInstance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private AccountEntity user;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPaid;

    @Column(nullable = false)
    private UUID checkoutSessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantStatus status = ParticipantStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    @Column
    private LocalDateTime transferredAt;

    @Column(name = "transfer_history", columnDefinition = "jsonb")
    @Convert(converter = TransferHistoryJsonConverter.class)
    private List<TransferHistory> transferHistory = new ArrayList<>();

    // NEW: Purchase history
    @Column(name = "purchase_history", columnDefinition = "jsonb")
    @Convert(converter = PurchaseHistoryJsonConverter.class)
    private List<PurchaseRecord> purchaseHistory = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }

    // Existing methods
    public boolean hasTransferred() {
        return transferHistory != null && !transferHistory.isEmpty();
    }

    public int getTransferCount() {
        return transferHistory != null ? transferHistory.size() : 0;
    }

    public void addTransferHistory(UUID fromGroupId, UUID toGroupId, String reason) {
        if (transferHistory == null) {
            transferHistory = new ArrayList<>();
        }

        TransferHistory history = TransferHistory.builder()
                .fromGroupId(fromGroupId)
                .toGroupId(toGroupId)
                .transferredAt(LocalDateTime.now())
                .reason(reason)
                .build();

        transferHistory.add(history);
        this.transferredAt = LocalDateTime.now();
    }

    // NEW: Purchase history methods
    public void addPurchaseRecord(
            UUID checkoutSessionId,
            Integer quantity,
            BigDecimal amountPaid,
            String transactionId
    ) {
        if (purchaseHistory == null) {
            purchaseHistory = new ArrayList<>();
        }

        PurchaseRecord record = PurchaseRecord.builder()
                .checkoutSessionId(checkoutSessionId)
                .quantity(quantity)
                .amountPaid(amountPaid)
                .purchasedAt(LocalDateTime.now())
                .transactionId(transactionId)
                .build();

        purchaseHistory.add(record);

        // Update aggregated totals
        this.quantity += quantity;
        this.totalPaid = this.totalPaid.add(amountPaid);
    }

    public int getPurchaseCount() {
        return purchaseHistory != null ? purchaseHistory.size() : 0;
    }

    public boolean hasMultiplePurchases() {
        return getPurchaseCount() > 1;
    }

    // Existing nested class
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferHistory {
        private UUID fromGroupId;
        private UUID toGroupId;
        private LocalDateTime transferredAt;
        private String reason;
    }

    // NEW: Nested class for purchase records
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseRecord {
        private UUID checkoutSessionId;
        private Integer quantity;
        private BigDecimal amountPaid;
        private LocalDateTime purchasedAt;
        private String transactionId;
    }
}