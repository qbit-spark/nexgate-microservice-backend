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
import org.nextgate.nextgatebackend.order_mng_service.service.OrderService;
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
 * Flow:
 * 1. Event published when group reaches full capacity
 * 2. Listener executes AFTER transaction commits (data persisted)
 * 3. Fetches fresh group and participants with user relationships
 * 4. Creates orders for each active participant with retry logic
 *
 * Executes asynchronously to avoid blocking the payment completion thread.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroupOrderCreationListener {

    private final OrderService orderService;
    private final GroupParticipantRepo participantRepo;
    private final GroupPurchaseInstanceRepo groupPurchaseInstanceRepo;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_DELAY_MS = 500;

    /**
     * Handles group completion event and creates orders for all participants.
     *
     * @TransactionalEventListener ensures this runs AFTER the transaction commits
     * @Async executes in separate thread pool
     * @Transactional creates new transaction for order creation
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
                log.warn("⚠️  No active participants found for group: {}", group.getGroupCode());
                return;
            }

            logGroupDetails(group, activeParticipants);

            // Process each participant
            OrderCreationResult result = processParticipants(activeParticipants);

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
     * Processes all participants and creates orders.
     */
    private OrderCreationResult processParticipants(
            List<GroupParticipantEntity> participants) {

        OrderCreationResult result = new OrderCreationResult();

        for (GroupParticipantEntity participant : participants) {
            processParticipant(participant, result);
        }

        return result;
    }

    /**
     * Processes a single participant and creates their order.
     */
    private void processParticipant(
            GroupParticipantEntity participant,
            OrderCreationResult result) {

        String userName = participant.getUser().getUserName();
        UUID participantId = participant.getParticipantId();
        UUID checkoutSessionId = participant.getCheckoutSessionId();

        log.info("Processing participant {}/{}: {} ({})",
                result.getTotalProcessed() + 1,
                result.getTotalParticipants(),
                userName,
                participantId);

        try {
            boolean success = createOrderWithRetry(checkoutSessionId, userName);

            if (success) {
                result.recordSuccess(userName);
                log.info("✓ Order created successfully for {}", userName);
            } else {
                result.recordFailure(userName, participantId);
                log.error("✗ Failed to create order for {} after {} retries",
                        userName, MAX_RETRY_ATTEMPTS);
            }

        } catch (Exception e) {
            result.recordFailure(userName, participantId);
            log.error("✗ Critical error creating order for {}: {}",
                    userName, e.getMessage(), e);
        }
    }

    // ========================================
    // ORDER CREATION WITH RETRY
    // ========================================

    /**
     * Creates order with exponential backoff retry strategy.
     *
     * Retry delays: 2s, 4s, 8s
     */
    private boolean createOrderWithRetry(UUID checkoutSessionId, String userName) {

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                log.debug("Order creation attempt {}/{} for {}",
                        attempt, MAX_RETRY_ATTEMPTS, userName);

                // Create order
                List<UUID> orderIds = orderService.createOrdersFromCheckoutSession(checkoutSessionId);
                UUID orderId = orderIds.get(0);

                log.info("✓ Order created: {} for {}", orderId, userName);
                return true;

            } catch (Exception e) {
                log.warn("Attempt {}/{} failed for {}: {}",
                        attempt, MAX_RETRY_ATTEMPTS, userName, e.getMessage());

                // Retry with backoff if not last attempt
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    waitBeforeRetry(attempt);
                } else {
                    log.error("All {} attempts failed for {}: {}",
                            MAX_RETRY_ATTEMPTS, userName, e.getMessage());
                }
            }
        }

        return false;
    }

    /**
     * Waits before retry with exponential backoff.
     * Attempt 1: 2s, Attempt 2: 4s, Attempt 3: 8s
     */
    private void waitBeforeRetry(int attempt) {
        try {
            long waitTime = (long) Math.pow(2, attempt) * 1000;
            log.debug("Waiting {}ms before retry", waitTime);
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Retry wait interrupted");
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
        log.info("║   HANDLING GROUP COMPLETION - ORDER CREATION          ║");
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
        log.info("Starting order creation process...");
    }

    private void logFinalSummary(
            GroupPurchaseInstanceEntity group,
            OrderCreationResult result) {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   ORDER CREATION COMPLETE                              ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Group: {} ({})", group.getGroupCode(), group.getGroupInstanceId());
        log.info("Total Participants: {}", result.getTotalParticipants());
        log.info("Orders Created: {} ✓", result.getSuccessCount());
        log.info("Failures: {} ✗", result.getFailureCount());

        if (result.hasFailures()) {
            logManualInterventionRequired(result);
        }
    }

    private void logManualInterventionRequired(OrderCreationResult result) {
        log.warn("╔════════════════════════════════════════════════════════╗");
        log.warn("║   ⚠️  MANUAL INTERVENTION REQUIRED  ⚠️                 ║");
        log.warn("╚════════════════════════════════════════════════════════╝");
        log.warn("Failed participants ({}):", result.getFailureCount());
        result.getFailedParticipants().forEach(name -> log.warn("  - {}", name));
        log.warn("Action Required: Manually create orders for failed participants");
    }

    private void logCriticalError(UUID groupInstanceId, Exception e) {
        log.error("╔════════════════════════════════════════════════════════╗");
        log.error("║   ⚠️  CRITICAL ERROR IN ORDER CREATION  ⚠️            ║");
        log.error("╚════════════════════════════════════════════════════════╝");
        log.error("Group: {}", groupInstanceId, e);
    }

    // ========================================
    // INNER CLASS: RESULT TRACKER
    // ========================================

    /**
     * Tracks order creation results for reporting.
     */
    @Getter
    private static class OrderCreationResult {
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