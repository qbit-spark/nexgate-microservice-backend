package org.nextgate.nextgatebackend.group_purchase_mng.payloads;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.nextgate.nextgatebackend.group_purchase_mng.enums.ParticipantStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupParticipantSummaryResponse {

    private UUID participantId;
    private UUID userId;
    private String userName;
    private String userProfilePicture;
    private Integer quantity;
    private ParticipantStatus status;
    private LocalDateTime joinedAt;
}