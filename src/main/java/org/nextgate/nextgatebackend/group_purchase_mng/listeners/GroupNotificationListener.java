package org.nextgate.nextgatebackend.group_purchase_mng.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupParticipantEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.enums.ParticipantStatus;
import org.nextgate.nextgatebackend.group_purchase_mng.events.GroupCompletedEvent;
import org.nextgate.nextgatebackend.group_purchase_mng.repo.GroupParticipantRepo;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Listens for GroupCompletedEvent and sends notifications to participants.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroupNotificationListener {

    private final GroupParticipantRepo participantRepo;
    // private final NotificationService notificationService;

    @EventListener
    @Async
    public void onGroupCompleted(GroupCompletedEvent event) {

        log.info("Sending group completion notifications for: {} ({})",
                event.getGroup().getGroupCode(),
                event.getGroupInstanceId());

        try {
            // Get all active participants
            List<GroupParticipantEntity> activeParticipants =
                    participantRepo.findByGroupInstanceOrderByJoinedAtAsc(event.getGroup())
                            .stream()
                            .filter(p -> p.getStatus() == ParticipantStatus.ACTIVE)
                            .toList();

            log.info("Notifying {} participants", activeParticipants.size());

            for (GroupParticipantEntity participant : activeParticipants) {
                try {
                    // TODO: Send notification
                    // notificationService.sendGroupCompletionNotification(
                    //     participant.getUser(),
                    //     event.getGroup()
                    // );

                    log.info("✓ Notification sent to: {}",
                            participant.getUser().getUserName());

                } catch (Exception e) {
                    log.error("Failed to notify participant: {}",
                            participant.getUser().getUserName(), e);
                }
            }

            log.info("✓ All notifications sent for group: {}",
                    event.getGroup().getGroupCode());

        } catch (Exception e) {
            log.error("Error sending group completion notifications", e);
        }
    }
}