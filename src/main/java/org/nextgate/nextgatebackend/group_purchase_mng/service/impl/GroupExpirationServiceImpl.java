package org.nextgate.nextgatebackend.group_purchase_mng.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Job;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.enums.GroupStatus;
import org.nextgate.nextgatebackend.group_purchase_mng.events.GroupFailedEvent;
import org.nextgate.nextgatebackend.group_purchase_mng.repo.GroupPurchaseInstanceRepo;
import org.nextgate.nextgatebackend.group_purchase_mng.service.GroupExpirationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupExpirationServiceImpl implements GroupExpirationService {

    private final GroupPurchaseInstanceRepo groupPurchaseInstanceRepo;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Expire a group purchase that didn't fill in time.
     * This method is called by scheduled JobRunr jobs.
     *
     * IMPORTANT: This method is idempotent - safe to call multiple times.
     */
    @Override
    @Job(name = "Expire Group Purchase", retries = 3)
    @Transactional
    public void expireGroup(UUID groupId) {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   EXECUTING GROUP EXPIRATION JOB                      ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Group ID: {}", groupId);

        // ========================================
        // 1. FETCH GROUP
        // ========================================
        GroupPurchaseInstanceEntity group = groupPurchaseInstanceRepo
                .findById(groupId)
                .orElse(null);

        if (group == null) {
            log.warn("⚠️  Group {} not found, skipping expiration", groupId);
            return;
        }

        log.info("Group Code: {}", group.getGroupCode());
        log.info("Product: {}", group.getProductName());
        log.info("Status: {}", group.getStatus());
        log.info("Seats: {}/{}", group.getSeatsOccupied(), group.getTotalSeats());
        log.info("Expires At: {}", group.getExpiresAt());

        // ========================================
        // 2. IDEMPOTENCY CHECKS
        // ========================================

        // Check 1: Already processed?
        if (group.getStatus() != GroupStatus.OPEN) {
            log.info("✓ Group already processed (status: {}), skipping", group.getStatus());
            return;
        }

        // Check 2: Group completed before expiration?
        if (group.isFull()) {
            log.info("✓ Group completed before expiration ({}/{}), skipping",
                    group.getSeatsOccupied(), group.getTotalSeats());
            return;
        }

        // Check 3: Not actually expired yet? (edge case)
        if (!group.isExpired()) {
            log.warn("⚠️  Group is not expired yet (expiresAt: {}), skipping",
                    group.getExpiresAt());
            return;
        }

        // ========================================
        // 3. PROCESS EXPIRATION
        // ========================================

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("Processing expiration for group: {}", group.getGroupCode());
        log.info("Participants: {}", group.getTotalParticipants());
        log.info("Unfilled seats: {}", group.getSeatsRemaining());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        LocalDateTime now = LocalDateTime.now();

        // Mark as FAILED
        group.setStatus(GroupStatus.FAILED);
        group.setUpdatedAt(now);

        // Save immediately
        groupPurchaseInstanceRepo.save(group);

        log.info("✓ Group marked as FAILED");

        // ========================================
        // 4. PUBLISH EVENT FOR ASYNC PROCESSING
        // ========================================

        try {
            GroupFailedEvent event = new GroupFailedEvent(
                    this,
                    group.getGroupInstanceId(),
                    group,
                    now
            );

            eventPublisher.publishEvent(event);

            log.info("✓ GroupFailedEvent published");
            log.info("  Event will trigger:");
            log.info("    - Refund processing for {} participants", group.getTotalParticipants());
            log.info("    - Notification to all participants");
            log.info("    - Shop owner notification");
            log.info("    - Analytics tracking");

        } catch (Exception e) {
            log.error("❌ Failed to publish GroupFailedEvent", e);

            // Don't throw - group is already marked as FAILED
            // Event can be recovered via scheduled cleanup job

            log.error("⚠️  MANUAL ACTION MAY BE REQUIRED:");
            log.error("   Group: {} ({})", group.getGroupCode(), group.getGroupInstanceId());
            log.error("   Status: FAILED but event not published");
            log.error("   Action: Verify refunds were issued to participants");
        }

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   GROUP EXPIRATION COMPLETE                           ║");
        log.info("╚════════════════════════════════════════════════════════╝");
    }
}