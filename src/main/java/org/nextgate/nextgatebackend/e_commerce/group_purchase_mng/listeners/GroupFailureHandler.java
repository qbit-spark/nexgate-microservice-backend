package org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.listeners;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.repo.CheckoutSessionRepo;
import org.nextgate.nextgatebackend.financial_system.escrow.service.EscrowService;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.entity.GroupParticipantEntity;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.enums.ParticipantStatus;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.events.GroupFailedEvent;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.repo.GroupParticipantRepo;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.repo.GroupPurchaseInstanceRepo;
import org.nextgate.nextgatebackend.notification_system.publisher.NotificationPublisher;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.NotificationEvent;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.Recipient;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationChannel;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationPriority;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationType;
import org.nextgate.nextgatebackend.notification_system.publisher.mapper.GroupNotificationMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Listens for GroupFailedEvent and handles refunds and notifications.
 *
 * Flow:
 * 1. Event published when group expires without filling
 * 2. Listener executes AFTER transaction commits
 * 3. Issues refunds to all active participants
 * 4. Marks participants as REFUNDED
 * 5. Sends notifications to participants and shop owner
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroupFailureHandler {

    private final GroupParticipantRepo participantRepo;
    private final GroupPurchaseInstanceRepo groupPurchaseInstanceRepo;
    private final NotificationPublisher notificationPublisher;
     private final EscrowService escrowService;
     private final CheckoutSessionRepo checkoutSessionRepo;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_DELAY_MS = 500;

    /**
     * Handles group failure event - issues refunds and sends notifications.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onGroupFailed(GroupFailedEvent event) {

        logEventReceived(event);

        // Small delay to ensure data consistency
        waitForDataConsistency();

        try {
            // Fetch fresh data
            GroupPurchaseInstanceEntity group = fetchGroup(event.getGroupInstanceId());
            List<GroupParticipantEntity> activeParticipants = fetchActiveParticipants(group);

            // Validate we have participants
            if (activeParticipants.isEmpty()) {
                log.info("No active participants to refund for group: {}", group.getGroupCode());
                return;
            }

            logGroupDetails(group, activeParticipants);

            // Process refunds for each participant
            RefundResult refundResult = processRefunds(activeParticipants);

            // Send notifications
            sendParticipantNotifications(group, activeParticipants, refundResult);
            sendShopOwnerNotification(group, activeParticipants);

            // Log final summary
            logFinalSummary(group, refundResult);

        } catch (Exception e) {
            logCriticalError(event.getGroupInstanceId(), e);
        }
    }

    // ========================================
    // CORE PROCESSING
    // ========================================

    private GroupPurchaseInstanceEntity fetchGroup(UUID groupInstanceId) {
        return groupPurchaseInstanceRepo.findById(groupInstanceId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupInstanceId));
    }

    private List<GroupParticipantEntity> fetchActiveParticipants(GroupPurchaseInstanceEntity group) {
        return participantRepo.findWithUserByGroup(group)
                .stream()
                .filter(p -> p.getStatus() == ParticipantStatus.ACTIVE)
                .toList();
    }

    /**
     * Process refunds for all active participants.
     */

    private RefundResult processRefunds(List<GroupParticipantEntity> participants) {

        RefundResult result = new RefundResult();
        result.setTotalParticipants(participants.size());

        for (GroupParticipantEntity participant : participants) {
            processParticipantRefund(participant, result);
        }

        return result;
    }

    /**
     * Process refund for a single participant.
     */
    private void processParticipantRefund(
            GroupParticipantEntity participant,
            RefundResult result) {

        String userName = participant.getUser().getUserName();
        UUID checkoutSessionId = participant.getCheckoutSessionId();

        CheckoutSessionEntity checkoutSession = checkoutSessionRepo.findBySessionId(checkoutSessionId).orElseThrow(
                () -> new RuntimeException("From refund process group failed Checkout session not found: " + checkoutSessionId)
        );

        UUID escrowId = checkoutSession.getEscrowId();

        log.info("Processing refund {}/{}: {} (checkoutSession: {})",
                result.getTotalProcessed() + 1,
                result.getTotalParticipants(),
                userName,
                checkoutSessionId);

        try {

            escrowService.refundMoney(escrowId);

            // For now, just mark as REFUNDED (simulate success)
            boolean success = markParticipantAsRefunded(participant);

            if (success) {
                result.recordSuccess(userName);
                log.info("✓ Refund processed successfully for {}", userName);
            } else {
                result.recordFailure(userName, participant.getParticipantId());
                log.error("✗ Failed to process refund for {}", userName);
            }

        } catch (Exception e) {
            result.recordFailure(userName, participant.getParticipantId());
            log.error("✗ Critical error processing refund for {}: {}",
                    userName, e.getMessage(), e);
        }
    }


    /**
     * Mark the participant as REFUNDED (actual refund logic to be implemented).
     */
    private boolean markParticipantAsRefunded(GroupParticipantEntity participant) {
        try {
            participant.setStatus(ParticipantStatus.REFUNDED);
            participantRepo.save(participant);
            return true;
        } catch (Exception e) {
            log.error("Failed to mark participant as refunded", e);
            return false;
        }
    }

    // ========================================
    // NOTIFICATION METHODS
    // ========================================

    private void sendParticipantNotifications(
            GroupPurchaseInstanceEntity group,
            List<GroupParticipantEntity> participants,
            RefundResult refundResult) {

        log.info("📧 Sending group failure notifications to {} participants",
                participants.size());

        int successCount = 0;
        int failureCount = 0;

        for (GroupParticipantEntity participant : participants) {
            try {
                sendParticipantNotification(group, participant);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to send notification to participant: {}",
                        participant.getUser().getUserName(), e);
                failureCount++;
            }
        }

        log.info("✅ Participant notifications sent: {} success, {} failed",
                successCount, failureCount);
    }

    private void sendParticipantNotification(
            GroupPurchaseInstanceEntity group,
            GroupParticipantEntity participant) {

        // Prepare notification data
        Map<String, Object> data = GroupNotificationMapper.mapGroupFailed(group, participant);

        // Build recipient
        Recipient recipient = Recipient.builder()
                .userId(participant.getUser().getId().toString())
                .email(participant.getUser().getEmail())
                .phone(participant.getUser().getPhoneNumber())
                .name(participant.getUser().getFirstName())
                .language("en")
                .build();

        // Create notification event
        NotificationEvent event = NotificationEvent.builder()
                .type(NotificationType.GROUP_PURCHASE_FAILED)
                .recipients(List.of(recipient))
                .channels(List.of(
                        NotificationChannel.EMAIL,
                        NotificationChannel.SMS,
                        NotificationChannel.PUSH,
                        NotificationChannel.IN_APP
                ))
                .priority(NotificationPriority.HIGH)
                .data(data)
                .build();

        // Publish notification
        notificationPublisher.publish(event);

        log.info("✓ Notification sent to: {}", participant.getUser().getUserName());
    }

    private void sendShopOwnerNotification(
            GroupPurchaseInstanceEntity group,
            List<GroupParticipantEntity> participants) {

        try {
            log.info("📧 Sending group failure notification to shop owner");

            AccountEntity shopOwner = group.getShop().getOwner();

            if (shopOwner == null) {
                log.warn("⚠️ Cannot send notification - shop has no owner");
                return;
            }

            // Prepare notification data
            Map<String, Object> data = GroupNotificationMapper.mapGroupFailedForShopOwner(
                    group, participants);

            // Build recipient
            Recipient recipient = Recipient.builder()
                    .userId(shopOwner.getId().toString())
                    .email(shopOwner.getEmail())
                    .phone(shopOwner.getPhoneNumber())
                    .name(shopOwner.getFirstName())
                    .language("en")
                    .build();

            // Create notification event
            NotificationEvent event = NotificationEvent.builder()
                    .type(NotificationType.GROUP_PURCHASE_FAILED)
                    .recipients(List.of(recipient))
                    .channels(List.of(
                            NotificationChannel.EMAIL,
                            NotificationChannel.IN_APP,
                            NotificationChannel.SMS
                    ))
                    .priority(NotificationPriority.NORMAL)
                    .data(data)
                    .build();

            // Publish notification
            notificationPublisher.publish(event);

            log.info("✅ Shop owner notification sent: {}", shopOwner.getUserName());

        } catch (Exception e) {
            log.error("❌ Failed to send shop owner notification", e);
        }
    }

    // ========================================
    // HELPER METHODS
    // ========================================

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

    private void logEventReceived(GroupFailedEvent event) {
        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   HANDLING GROUP FAILURE - REFUND PROCESSING          ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Group ID: {}", event.getGroupInstanceId());
        log.info("Failed At: {}", event.getFailedAt());
    }

    private void logGroupDetails(
            GroupPurchaseInstanceEntity group,
            List<GroupParticipantEntity> participants) {

        log.info("Group Code: {}", group.getGroupCode());
        log.info("Product: {}", group.getProductName());
        log.info("Active Participants: {}", participants.size());
        log.info("Seats Filled: {}/{}", group.getSeatsOccupied(), group.getTotalSeats());
        log.info("Starting refund processing...");
    }

    private void logFinalSummary(
            GroupPurchaseInstanceEntity group,
            RefundResult result) {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   REFUND PROCESSING COMPLETE                          ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Group: {} ({})", group.getGroupCode(), group.getGroupInstanceId());
        log.info("Total Participants: {}", result.getTotalParticipants());
        log.info("Refunds Processed: {} ✓", result.getSuccessCount());
        log.info("Failures: {} ✗", result.getFailureCount());

        if (result.hasFailures()) {
            logManualInterventionRequired(result);
        }
    }

    private void logManualInterventionRequired(RefundResult result) {
        log.warn("╔════════════════════════════════════════════════════════╗");
        log.warn("║   ⚠️  MANUAL INTERVENTION REQUIRED  ⚠️                 ║");
        log.warn("╚════════════════════════════════════════════════════════╝");
        log.warn("Failed refunds ({}):", result.getFailureCount());
        result.getFailedParticipants().forEach(name -> log.warn("  - {}", name));
        log.warn("Action Required: Manually process refunds for failed participants");
    }

    private void logCriticalError(UUID groupInstanceId, Exception e) {
        log.error("╔════════════════════════════════════════════════════════╗");
        log.error("║   ⚠️  CRITICAL ERROR IN REFUND PROCESSING  ⚠️         ║");
        log.error("╚════════════════════════════════════════════════════════╝");
        log.error("Group: {}", groupInstanceId, e);
    }

    // ========================================
    // INNER CLASS: RESULT TRACKER
    // ========================================

    @Getter
    private static class RefundResult {
        private int successCount = 0;
        private int failureCount = 0;
        private final List<String> failedParticipants = new ArrayList<>();
        private int totalParticipants = 0;

        public void recordSuccess(String userName) {
            successCount++;
        }

        public void recordFailure(String userName, UUID participantId) {
            failureCount++;
            failedParticipants.add(userName + " (" + participantId + ")");
        }

        public int getTotalProcessed() {
            return successCount + failureCount;
        }

        public void setTotalParticipants(int total) {
            this.totalParticipants = total;
        }

        public boolean hasFailures() {
            return failureCount > 0;
        }
    }
}