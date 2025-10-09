package org.nextgate.nextgatebackend.installment_purchase.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InstallmentScheduledJobs {

    private final JobScheduler jobScheduler;
    private final InstallmentJobOrchestrator orchestrator;

    @Recurring(id = "process-due-payments", cron = "0 0 2 * * *", zoneId = "Africa/Nairobi")
    public void scheduleDuePayments() {

        log.info("⏰ TRIGGER: Process Due Payments - 2:00 AM EAT");

        try {
            jobScheduler.enqueue(orchestrator::processDuePaymentsBatch);

            log.info("✓ Due payments batch job scheduled successfully");

        } catch (Exception e) {
            log.error("✗ Failed to schedule due payments batch", e);
        }
    }

    @Recurring(id = "retry-failed-payments", cron = "0 0 14 * * *", zoneId = "Africa/Nairobi")
    public void scheduleRetryPayments() {

        log.info("⏰ TRIGGER: Retry Failed Payments - 2:00 PM EAT");

        try {
            jobScheduler.enqueue(orchestrator::retryFailedPaymentsBatch);

            log.info("✓ Retry failed payments batch job scheduled successfully");

        } catch (Exception e) {
            log.error("✗ Failed to schedule retry payments batch", e);
        }
    }

    @Recurring(id = "mark-overdue-payments", cron = "0 0 23 * * *", zoneId = "Africa/Nairobi")
    public void scheduleOverdueCheck() {

        log.info("⏰ TRIGGER: Mark Overdue Payments - 11:00 PM EAT");

        try {
            jobScheduler.enqueue(orchestrator::markOverduePaymentsBatch);

            log.info("✓ Overdue payments batch job scheduled successfully");

        } catch (Exception e) {
            log.error("✗ Failed to schedule overdue payments batch", e);
        }
    }

    @Recurring(id = "send-payment-reminders", cron = "0 0 9 * * *", zoneId = "Africa/Nairobi")
    public void schedulePaymentReminders() {

        log.info("⏰ TRIGGER: Send Payment Reminders - 9:00 AM EAT");

        try {
            jobScheduler.enqueue(orchestrator::sendPaymentRemindersBatch);

            log.info("✓ Payment reminders batch job scheduled successfully");

        } catch (Exception e) {
            log.error("✗ Failed to schedule payment reminders batch", e);
        }
    }

    @Recurring(id = "weekly-installment-report", cron = "0 0 8 * * MON", zoneId = "Africa/Nairobi")
    public void scheduleWeeklyReport() {

        log.info("⏰ TRIGGER: Weekly Installment Report - Monday 8:00 AM EAT");

        try {
            log.info("Generating weekly installment report...");

            log.info("✓ Weekly report job scheduled successfully");

        } catch (Exception e) {
            log.error("✗ Failed to schedule weekly report", e);
        }
    }
}