package org.nextgate.nextgatebackend.group_purchase_mng.payloads;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.nextgate.nextgatebackend.group_purchase_mng.enums.GroupStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupPurchaseSummaryResponse {

    private UUID groupInstanceId;
    private String groupCode;

    private String productName;
    private String productImage;

    private String shopName;

    private BigDecimal groupPrice;
    private BigDecimal savingsPercentage;
    private String currency;

    private Integer totalSeats;
    private Integer seatsOccupied;
    private Integer seatsRemaining;
    private Integer totalParticipants;

    private Double progressPercentage;

    private GroupStatus status;

    private LocalDateTime expiresAt;
    private Boolean isExpired;

    private Boolean isUserMember;

    private List<ParticipantPreview> participants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ParticipantPreview {
        private UUID userId;
        private String userName;
        private String userProfilePicture;
        private Integer quantity;
        private Double contributionPercentage;
    }
}