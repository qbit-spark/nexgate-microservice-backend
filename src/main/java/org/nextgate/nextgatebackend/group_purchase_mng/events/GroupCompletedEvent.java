package org.nextgate.nextgatebackend.group_purchase_mng.events;

import lombok.Getter;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a group purchase reaches full capacity
 * and is marked as COMPLETED.
 *
 * Triggers:
 * - Order creation for all participants
 * - Notification to all participants
 * - Analytics tracking
 */
@Getter
public class GroupCompletedEvent extends ApplicationEvent {

    private final UUID groupInstanceId;
    private final GroupPurchaseInstanceEntity group;
    private final LocalDateTime completedAt;
    private final Integer totalParticipants;
    private final Integer totalSeats;

    public GroupCompletedEvent(
            Object source,
            UUID groupInstanceId,
            GroupPurchaseInstanceEntity group,
            LocalDateTime completedAt
    ) {
        super(source);
        this.groupInstanceId = groupInstanceId;
        this.group = group;
        this.completedAt = completedAt;
        this.totalParticipants = group.getTotalParticipants();
        this.totalSeats = group.getTotalSeats();
    }
}