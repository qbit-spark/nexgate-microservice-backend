package org.nextgate.nextgatebackend.group_purchase_mng.events;

import lombok.Getter;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a group purchase expires without filling.
 *
 * Triggers:
 * - Refund processing for all participants
 * - Notification to all participants
 * - Shop owner notification
 * - Analytics tracking
 */
@Getter
public class GroupFailedEvent extends ApplicationEvent {

    private final UUID groupInstanceId;
    private final GroupPurchaseInstanceEntity group;
    private final LocalDateTime failedAt;
    private final Integer totalParticipants;
    private final Integer seatsOccupied;
    private final Integer seatsUnfilled;

    public GroupFailedEvent(
            Object source,
            UUID groupInstanceId,
            GroupPurchaseInstanceEntity group,
            LocalDateTime failedAt
    ) {
        super(source);
        this.groupInstanceId = groupInstanceId;
        this.group = group;
        this.failedAt = failedAt;
        this.totalParticipants = group.getTotalParticipants();
        this.seatsOccupied = group.getSeatsOccupied();
        this.seatsUnfilled = group.getSeatsRemaining();
    }
}