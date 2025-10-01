package org.nextgate.nextgatebackend.group_purchase_mng.payloads;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.nextgate.nextgatebackend.group_purchase_mng.enums.ParticipantStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupParticipantResponse {

    private UUID participantId;
    private UUID userId;
    private String userName;
    private String userProfilePicture;
    private Integer quantity;
    private BigDecimal totalPaid;
    private ParticipantStatus status;
    private LocalDateTime joinedAt;
    private Integer purchaseCount;
    private Boolean hasTransferred;
    private UUID checkoutSessionId;

    private List<PurchaseHistoryItem> purchaseHistory;
    private List<TransferHistoryItem> transferHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PurchaseHistoryItem {
        private UUID checkoutSessionId;
        private Integer quantity;
        private BigDecimal amountPaid;
        private LocalDateTime purchasedAt;
        private String transactionId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TransferHistoryItem {
        private UUID fromGroupId;
        private String fromGroupCode;
        private UUID toGroupId;
        private String toGroupCode;
        private LocalDateTime transferredAt;
        private String reason;
    }
}