package org.nextgate.nextgatebackend.group_purchase_mng.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupParticipantEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.enums.ParticipantStatus;
import org.nextgate.nextgatebackend.group_purchase_mng.events.GroupCompletedEvent;
import org.nextgate.nextgatebackend.group_purchase_mng.repo.GroupParticipantRepo;
import org.nextgate.nextgatebackend.group_purchase_mng.repo.GroupPurchaseInstanceRepo;
import org.nextgate.nextgatebackend.order_mng_service.service.OrderService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Listens for GroupCompletedEvent and creates orders for all participants.
 *
 * Executes asynchronously to avoid blocking the user request.
 * Implements retry logic for failed order creations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroupOrderCreationListener {

    private final OrderService orderService;
    private final GroupParticipantRepo participantRepo;
    private final GroupPurchaseInstanceRepo groupPurchaseInstanceRepo;


    @EventListener
    @Async
    @Transactional(readOnly = true)
    public void onGroupCompleted(GroupCompletedEvent event) {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   HANDLING GROUP COMPLETION - ORDER CREATION          ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Group ID: {}", event.getGroupInstanceId());

        try {
            // Fetch fresh group with active session
            GroupPurchaseInstanceEntity group = groupPurchaseInstanceRepo
                    .findById(event.getGroupInstanceId())
                    .orElseThrow(() -> new RuntimeException(
                            "Group not found: " + event.getGroupInstanceId()));

            log.info("Group Code: {}", group.getGroupCode());
            log.info("Product: {}", group.getProductName());
            log.info("Completed At: {}", group.getCompletedAt());

            // Get all active participants
            List<GroupParticipantEntity> allParticipants =
                    participantRepo.findByGroupInstanceOrderByJoinedAtAsc(group);

            List<GroupParticipantEntity> activeParticipants = allParticipants.stream()
                    .filter(p -> p.getStatus() == ParticipantStatus.ACTIVE)
                    .toList();

            log.info("Total Participants: {}", allParticipants.size());
            log.info("Active Participants: {}", activeParticipants.size());

            if (activeParticipants.isEmpty()) {
                log.warn("No active participants found for group: {}",
                        event.getGroupInstanceId());
                return;
            }

            // Create orders for each participant
            int successCount = 0;
            int failCount = 0;
            List<String> failedParticipants = new ArrayList<>();

            for (GroupParticipantEntity participant : activeParticipants) {

                String userName = participant.getUser().getUserName();
                UUID participantId = participant.getParticipantId();
                UUID checkoutSessionId = participant.getCheckoutSessionId();

                log.info("Processing participant #{}/{}: {} ({})",
                        successCount + failCount + 1,
                        activeParticipants.size(),
                        userName,
                        participantId);

                try {
                    boolean orderCreated = createOrderWithRetry(
                            checkoutSessionId,
                            userName,
                            3
                    );

                    if (orderCreated) {
                        successCount++;
                        log.info("✓ Order created successfully for {}", userName);
                    } else {
                        failCount++;
                        failedParticipants.add(userName + " (" + participantId + ")");
                        log.error("✗ Failed to create order for {} after retries",
                                userName);
                    }

                } catch (Exception e) {
                    failCount++;
                    failedParticipants.add(userName + " (" + participantId + ")");
                    log.error("✗ Critical error creating order for {}: {}",
                            userName, e.getMessage(), e);
                }
            }

            // Log summary
            log.info("╔════════════════════════════════════════════════════════╗");
            log.info("║   ORDER CREATION COMPLETE                              ║");
            log.info("╚════════════════════════════════════════════════════════╝");
            log.info("Group: {} ({})",
                    group.getGroupCode(),
                    event.getGroupInstanceId());
            log.info("Total Participants: {}", activeParticipants.size());
            log.info("Orders Created: {} ✓", successCount);
            log.info("Failures: {} ✗", failCount);

            if (failCount > 0) {
                log.warn("╔════════════════════════════════════════════════════════╗");
                log.warn("║   ⚠ MANUAL INTERVENTION REQUIRED ⚠                    ║");
                log.warn("╚════════════════════════════════════════════════════════╝");
                log.warn("Failed participants ({}):", failCount);
                failedParticipants.forEach(name -> log.warn("  - {}", name));
            }

        } catch (Exception e) {
            log.error("╔════════════════════════════════════════════════════════╗");
            log.error("║   ⚠ CRITICAL ERROR IN ORDER CREATION ⚠                ║");
            log.error("╚════════════════════════════════════════════════════════╝");
            log.error("Group: {}", event.getGroupInstanceId(), e);
        }
    }

    // ========================================
    // HELPER METHOD: CREATE ORDER WITH RETRY
    // ========================================

    private boolean createOrderWithRetry(
            UUID checkoutSessionId,
            String userName,
            int maxAttempts
    ) {

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.debug("Order creation attempt {}/{} for {}",
                        attempt, maxAttempts, userName);

                List<UUID> orderIds = orderService
                        .createOrdersFromCheckoutSession(checkoutSessionId);

                UUID orderId = orderIds.getFirst();

                log.info("✓ Order created: {} for {}", orderId, userName);

                return true;

            } catch (Exception e) {
                log.warn("Attempt {}/{} failed for {}: {}",
                        attempt, maxAttempts, userName, e.getMessage());

                if (attempt < maxAttempts) {
                    // Wait before retry (exponential backoff)
                    try {
                        long waitTime = (long) Math.pow(2, attempt) * 1000; // 2s, 4s, 8s
                        log.debug("Waiting {}ms before retry", waitTime);
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Retry wait interrupted");
                        return false;
                    }
                } else {
                    log.error("All {} attempts failed for {}", maxAttempts, userName);
                }
            }
        }

        return false;
    }
}