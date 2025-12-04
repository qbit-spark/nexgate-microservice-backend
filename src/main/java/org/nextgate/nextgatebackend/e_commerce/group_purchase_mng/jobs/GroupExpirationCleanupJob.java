package org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.enums.GroupStatus;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.repo.GroupPurchaseInstanceRepo;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.service.GroupExpirationService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Safety net cleanup job that catches any groups that missed their scheduled expiration.
 * Runs every night at midnight (00:00).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroupExpirationCleanupJob {

    private final GroupPurchaseInstanceRepo groupPurchaseInstanceRepo;
    private final GroupExpirationService groupExpirationService;

    /**
     * Runs every night at midnight to catch missed expirations.
     * Cron: "0 0 0 * * *" = At 00:00:00 every day
     */
    @Recurring(id = "cleanup-expired-groups", cron = "0 0 0 * * *")
    @Job(name = "Cleanup Expired Groups - Nightly", retries = 2)
    public void cleanupExpiredGroups() {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   RUNNING NIGHTLY EXPIRATION CLEANUP JOB              ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Time: {}", LocalDateTime.now());

        LocalDateTime now = LocalDateTime.now();

        // Find all groups that are:
        // - Status = OPEN
        // - expiresAt < now (expired)
        // - Not deleted
        List<GroupPurchaseInstanceEntity> expiredGroups =
                groupPurchaseInstanceRepo.findByStatusAndExpiresAtBeforeAndIsDeletedFalse(
                        GroupStatus.OPEN,
                        now
                );

        if (expiredGroups.isEmpty()) {
            log.info("✅ No expired groups found - all scheduled jobs working correctly");
            log.info("═══════════════════════════════════════════════════════");
            return;
        }

        log.warn("⚠️  Found {} groups that missed their scheduled expiration",
                expiredGroups.size());
        log.warn("This should be rare - investigating...");

        int successCount = 0;
        int failureCount = 0;

        for (GroupPurchaseInstanceEntity group : expiredGroups) {
            try {
                log.info("Processing missed expiration for group: {} (code: {})",
                        group.getGroupInstanceId(), group.getGroupCode());
                log.info("  Expired at: {}", group.getExpiresAt());
                log.info("  Hours overdue: {}",
                        java.time.Duration.between(group.getExpiresAt(), now).toHours());

                // Call the same expiration service (idempotent)
                groupExpirationService.expireGroup(group.getGroupInstanceId());

                successCount++;
                log.info("✓ Successfully expired group: {}", group.getGroupCode());

            } catch (Exception e) {
                log.error("✗ Failed to expire group: {}", group.getGroupCode(), e);
                failureCount++;
            }
        }

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   NIGHTLY CLEANUP COMPLETE                            ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Total Expired Groups Found: {}", expiredGroups.size());
        log.info("Successfully Processed: {} ✓", successCount);
        log.info("Failed: {} ✗", failureCount);

        if (failureCount > 0) {
            log.error("⚠️  MANUAL INTERVENTION MAY BE REQUIRED");
            log.error("   {} groups failed to expire", failureCount);
            log.error("   Check logs for details and process manually");
        }

        if (expiredGroups.size() > 5) {
            log.warn("⚠️  ALERT: High number of missed expirations ({})!", expiredGroups.size());
            log.warn("   This suggests a problem with scheduled jobs.");
            log.warn("   Possible causes:");
            log.warn("   - JobRunr server was down");
            log.warn("   - Database issues");
            log.warn("   - Jobs being deleted incorrectly");
        }

        log.info("═══════════════════════════════════════════════════════");
    }
}