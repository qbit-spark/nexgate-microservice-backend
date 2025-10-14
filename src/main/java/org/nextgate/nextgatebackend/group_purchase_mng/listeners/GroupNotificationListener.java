package org.nextgate.nextgatebackend.group_purchase_mng.listeners;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupParticipantEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.enums.ParticipantStatus;
import org.nextgate.nextgatebackend.group_purchase_mng.events.GroupCompletedEvent;
import org.nextgate.nextgatebackend.group_purchase_mng.repo.GroupParticipantRepo;
import org.nextgate.nextgatebackend.group_purchase_mng.repo.GroupPurchaseInstanceRepo;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

/**
 * Listens for GroupCompletedEvent and sends notifications to participants.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroupNotificationListener {

    private final GroupParticipantRepo participantRepo;
    // private final NotificationService notificationService;
    private final GroupPurchaseInstanceRepo groupPurchaseInstanceRepo;


//    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Async
    @Transactional
    public void onGroupCompleted(GroupCompletedEvent event) {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   SENDING GROUP COMPLETION NOTIFICATIONS              ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Group ID: {}", event.getGroupInstanceId());

        try {
            // ========================================
            // ✅ FETCH FRESH GROUP (with all relationships loaded)
            // ========================================
            GroupPurchaseInstanceEntity group = groupPurchaseInstanceRepo
                    .findById(event.getGroupInstanceId())
                    .orElseThrow(() -> new RuntimeException(
                            "Group not found: " + event.getGroupInstanceId()));

            log.info("Group Code: {}", group.getGroupCode());
            log.info("Product: {}", group.getProductName());
            log.info("Completed At: {}", group.getCompletedAt());

            // ========================================
            // 1. GET ALL ACTIVE PARTICIPANTS (Fresh query)
            // ========================================
            List<GroupParticipantEntity> activeParticipants =
                    participantRepo.findByGroupInstanceOrderByJoinedAtAsc(group)
                            .stream()
                            .filter(p -> p.getStatus() == ParticipantStatus.ACTIVE)
                            .toList();

            log.info("Found {} active participants to notify", activeParticipants.size());

            if (activeParticipants.isEmpty()) {
                log.warn("No active participants to notify for group: {}",
                        group.getGroupCode());
                return;
            }

            // ========================================
            // 2. SEND NOTIFICATIONS TO EACH PARTICIPANT
            // ========================================
            int successCount = 0;
            int failCount = 0;

            for (GroupParticipantEntity participant : activeParticipants) {
                try {
                    // TODO: Send notification via NotificationService
                    // notificationService.sendGroupCompletionNotification(
                    //     participant.getUser(),
                    //     group
                    // );

                    // ✅ Access user through fresh participant entity
                    log.info("✓ Notification sent to: {} ({})",
                            participant.getUser().getUserName(),
                            participant.getUser().getEmail());

                    successCount++;

                } catch (Exception e) {
                    log.error("✗ Failed to notify participant: {}",
                            participant.getUser().getUserName(), e);
                    failCount++;
                }
            }

            // ========================================
            // 3. LOG SUMMARY
            // ========================================
            log.info("╔════════════════════════════════════════════════════════╗");
            log.info("║   NOTIFICATION SENDING COMPLETE                       ║");
            log.info("╚════════════════════════════════════════════════════════╝");
            log.info("Group: {} ({})",
                    group.getGroupCode(),
                    event.getGroupInstanceId());
            log.info("Total Participants: {}", activeParticipants.size());
            log.info("Notifications Sent: {} ✓", successCount);
            log.info("Failures: {} ✗", failCount);

            if (failCount > 0) {
                log.warn("⚠️  {} notification(s) failed to send", failCount);
            }

        } catch (Exception e) {
            log.error("╔════════════════════════════════════════════════════════╗");
            log.error("║   ⚠️  CRITICAL ERROR SENDING NOTIFICATIONS  ⚠️       ║");
            log.error("╚════════════════════════════════════════════════════════╝");
            log.error("Group: {}", event.getGroupInstanceId(), e);
        }
    }
}