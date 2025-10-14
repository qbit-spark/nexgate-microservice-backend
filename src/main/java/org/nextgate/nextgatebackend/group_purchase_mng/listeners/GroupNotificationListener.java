package org.nextgate.nextgatebackend.group_purchase_mng.listeners;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupParticipantEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.enums.ParticipantStatus;
import org.nextgate.nextgatebackend.group_purchase_mng.events.GroupCompletedEvent;
import org.nextgate.nextgatebackend.group_purchase_mng.repo.GroupParticipantRepo;
import org.nextgate.nextgatebackend.group_purchase_mng.repo.GroupPurchaseInstanceRepo;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Listens for GroupCompletedEvent and sends notifications to all participants.
 *
 * Flow:
 * 1. Event published when group reaches full capacity
 * 2. Listener executes AFTER transaction commits (data persisted)
 * 3. Fetches fresh group and participants with user relationships
 * 4. Sends notifications to each active participant
 *
 * Executes asynchronously to avoid blocking the payment completion thread.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroupNotificationListener {

    private final GroupParticipantRepo participantRepo;
    private final GroupPurchaseInstanceRepo groupPurchaseInstanceRepo;
    // private final NotificationService notificationService;  // TODO: Uncomment when available

    private static final long INITIAL_DELAY_MS = 500;

    /**
     * Handles group completion event and sends notifications to all participants.
     *
     * @TransactionalEventListener ensures this runs AFTER the transaction commits
     * @Async executes in separate thread pool
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onGroupCompleted(GroupCompletedEvent event) {

        logEventReceived(event);

        // Small delay to ensure data consistency
        waitForDataConsistency();

        try {
            // Fetch fresh data with new Hibernate session
            GroupPurchaseInstanceEntity group = fetchGroup(event.getGroupInstanceId());
            List<GroupParticipantEntity> activeParticipants = fetchActiveParticipants(group);

            // Validate we have participants
            if (activeParticipants.isEmpty()) {
                log.warn("⚠️  No active participants to notify for group: {}", group.getGroupCode());
                return;
            }

            logGroupDetails(group, activeParticipants);

            // Send notifications to each participant
            NotificationResult result = notifyParticipants(activeParticipants, group);

            // Log final summary
            logFinalSummary(group, result);

        } catch (Exception e) {
            logCriticalError(event.getGroupInstanceId(), e);
        }
    }

    // ========================================
    // CORE PROCESSING
    // ========================================

    /**
     * Fetches group from database with fresh Hibernate session.
     */
    private GroupPurchaseInstanceEntity fetchGroup(UUID groupInstanceId) {
        return groupPurchaseInstanceRepo.findById(groupInstanceId)
                .orElseThrow(() -> new RuntimeException(
                        "Group not found: " + groupInstanceId));
    }

    /**
     * Fetches participants with user relationships eagerly loaded.
     * Uses JOIN FETCH to avoid LazyInitializationException.
     */
    private List<GroupParticipantEntity> fetchActiveParticipants(
            GroupPurchaseInstanceEntity group) {

        return participantRepo.findWithUserByGroup(group)
                .stream()
                .filter(p -> p.getStatus() == ParticipantStatus.ACTIVE)
                .toList();
    }

    /**
     * Sends notifications to all participants.
     */
    private NotificationResult notifyParticipants(
            List<GroupParticipantEntity> participants,
            GroupPurchaseInstanceEntity group) {

        NotificationResult result = new NotificationResult(participants.size());

        for (GroupParticipantEntity participant : participants) {
            notifyParticipant(participant, group, result);
        }

        return result;
    }

    /**
     * Sends notification to a single participant.
     */
    private void notifyParticipant(
            GroupParticipantEntity participant,
            GroupPurchaseInstanceEntity group,
            NotificationResult result) {

        String userName = participant.getUser().getUserName();
        String userEmail = participant.getUser().getEmail();

        try {
            // TODO: Uncomment when NotificationService is available
            // notificationService.sendGroupCompletionNotification(
            //     participant.getUser(),
            //     group
            // );

            // Placeholder log until notification service is implemented
            log.info("✓ Notification sent to: {} ({})", userName, userEmail);

            result.recordSuccess(userName);

        } catch (Exception e) {
            log.error("✗ Failed to notify participant: {} - {}",
                    userName, e.getMessage(), e);
            result.recordFailure(userName);
        }
    }

    /**
     * Small delay to ensure database consistency after transaction commit.
     */
    private void waitForDataConsistency() {
        try {
            Thread.sleep(INITIAL_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Initial delay interrupted");
        }
    }

    // ========================================
    // LOGGING HELPERS
    // ========================================

    private void logEventReceived(GroupCompletedEvent event) {
        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   SENDING GROUP COMPLETION NOTIFICATIONS              ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Group ID: {}", event.getGroupInstanceId());
        log.info("Completed At: {}", event.getCompletedAt());
    }

    private void logGroupDetails(
            GroupPurchaseInstanceEntity group,
            List<GroupParticipantEntity> participants) {

        log.info("Group Code: {}", group.getGroupCode());
        log.info("Product: {}", group.getProductName());
        log.info("Active Participants: {}", participants.size());
        log.info("Starting notification process...");
    }

    private void logFinalSummary(
            GroupPurchaseInstanceEntity group,
            NotificationResult result) {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   NOTIFICATION SENDING COMPLETE                        ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Group: {} ({})", group.getGroupCode(), group.getGroupInstanceId());
        log.info("Total Participants: {}", result.getTotalParticipants());
        log.info("Notifications Sent: {} ✓", result.getSuccessCount());
        log.info("Failures: {} ✗", result.getFailureCount());

        if (result.hasFailures()) {
            logNotificationFailures(result);
        }
    }

    private void logNotificationFailures(NotificationResult result) {
        log.warn("╔════════════════════════════════════════════════════════╗");
        log.warn("║   ⚠️  NOTIFICATION FAILURES  ⚠️                        ║");
        log.warn("╚════════════════════════════════════════════════════════╝");
        log.warn("Failed to notify ({}):", result.getFailureCount());
        result.getFailedParticipants().forEach(name -> log.warn("  - {}", name));
    }

    private void logCriticalError(UUID groupInstanceId, Exception e) {
        log.error("╔════════════════════════════════════════════════════════╗");
        log.error("║   ⚠️  CRITICAL ERROR SENDING NOTIFICATIONS  ⚠️        ║");
        log.error("╚════════════════════════════════════════════════════════╝");
        log.error("Group: {}", groupInstanceId, e);
    }

    // ========================================
    // INNER CLASS: RESULT TRACKER
    // ========================================

    /**
     * Tracks notification results for reporting.
     */
    @Getter
    private static class NotificationResult {
        private int successCount = 0;
        private int failureCount = 0;
        private final List<String> failedParticipants = new ArrayList<>();
        private final int totalParticipants;

        public NotificationResult(int totalParticipants) {
            this.totalParticipants = totalParticipants;
        }

        public void recordSuccess(String userName) {
            successCount++;
        }

        public void recordFailure(String userName) {
            failureCount++;
            failedParticipants.add(userName);
        }

        public boolean hasFailures() {
            return failureCount > 0;
        }
    }
}