package org.nextgate.nextgatebackend.group_purchase_mng.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.enums.ParticipantStatus;
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

    // ========================================
    // PRIMARY IDENTIFICATION
    // ========================================

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID participantId;

    // ========================================
    // RELATIONSHIPS
    // ========================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_instance_id", referencedColumnName = "groupInstanceId", nullable = false)
    private GroupPurchaseInstanceEntity groupInstance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private AccountEntity user;

    // ========================================
    // PURCHASE DETAILS
    // ========================================

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPaid;

    @Column(nullable = false)
    private UUID checkoutSessionId;

    // ========================================
    // STATUS & TIMING
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantStatus status = ParticipantStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    @Column
    private LocalDateTime transferredAt;

    // ========================================
    // TRANSFER HISTORY (JSON)
    // ========================================

    @Column(name = "transfer_history", columnDefinition = "jsonb")
    @Convert(converter = TransferHistoryJsonConverter.class)
    private List<TransferHistory> transferHistory = new ArrayList<>();

    // ========================================
    // LIFECYCLE HOOKS
    // ========================================

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }

    // ========================================
    // BUSINESS LOGIC METHODS
    // ========================================

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

    // ========================================
    // NESTED CLASS FOR JSON STORAGE
    // ========================================

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
}