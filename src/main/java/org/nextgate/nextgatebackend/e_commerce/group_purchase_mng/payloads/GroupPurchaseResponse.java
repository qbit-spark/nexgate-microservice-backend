package org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.payloads;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.enums.GroupStatus;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.enums.ParticipantStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupPurchaseResponse {

    private UUID groupInstanceId;
    private String groupCode;

    private UUID productId;
    private String productName;
    private String productImage;

    private UUID shopId;
    private String shopName;
    private String shopLogo;

    private BigDecimal regularPrice;
    private BigDecimal groupPrice;
    private BigDecimal savingsAmount;
    private BigDecimal savingsPercentage;
    private String currency;

    private Integer totalSeats;
    private Integer seatsOccupied;
    private Integer seatsRemaining;
    private Integer totalParticipants;
    private Double progressPercentage;

    private GroupStatus status;
    private Boolean isExpired;
    private Boolean isFull;

    private UUID initiatorId;
    private String initiatorName;

    private Integer durationHours;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime completedAt;

    private Integer maxPerCustomer;

    private Boolean isUserMember;
    private UUID myParticipantId;
    private Integer myQuantity;

    private List<ParticipantDetail> participants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ParticipantDetail {
        private UUID participantId;
        private UUID userId;
        private String userName;
        private String userProfilePicture;
        private Integer quantity;
        private BigDecimal totalPaid;
        private ParticipantStatus status;
        private LocalDateTime joinedAt;
        private Double contributionPercentage;
        private Integer purchaseCount;
        private Boolean hasTransferred;

        private List<PurchaseHistoryItem> purchaseHistory;
        private List<TransferHistoryItem> transferHistory;
    }

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