package org.nextgate.nextgatebackend.installment_purchase.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.scheduling.JobScheduler;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentPaymentEntity;
import org.nextgate.nextgatebackend.installment_purchase.enums.PaymentStatus;
import org.nextgate.nextgatebackend.installment_purchase.service.InstallmentService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InstallmentJobOrchestrator {

    private final InstallmentService installmentService;
    private final JobScheduler jobScheduler;
    private final InstallmentPaymentProcessor paymentProcessor;

    public void processDuePaymentsBatch() {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║  PROCESSING DUE PAYMENTS BATCH                             ║");
        log.info("╚════════════════════════════════════════════════════════════╝");

        try {
            List<InstallmentPaymentEntity> duePayments = installmentService.getPaymentsDueToday();

            log.info("Found {} payments due today", duePayments.size());

            if (duePayments.isEmpty()) {
                log.info("No payments to process today");
                return;
            }

            int successCount = 0;
            int failedCount = 0;

            for (InstallmentPaymentEntity payment : duePayments) {
                try {
                    jobScheduler.enqueue(() ->
                            paymentProcessor.processPayment(payment.getPaymentId())
                    );
                    successCount++;

                    log.debug("Enqueued payment job: {} - Amount: {} TZS",
                            payment.getPaymentId(),
                            payment.getScheduledAmount());

                } catch (Exception e) {
                    failedCount++;
                    log.error("Failed to enqueue payment job: {}",
                            payment.getPaymentId(), e);
                }
            }

            log.info("Batch processing summary:");
            log.info("  Total payments: {}", duePayments.size());
            log.info("  Successfully enqueued: {}", successCount);
            log.info("  Failed to enqueue: {}", failedCount);
            log.info("╚════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            log.error("Error in processDuePaymentsBatch", e);
            throw new RuntimeException("Failed to process due payments batch", e);
        }
    }

    public void retryFailedPaymentsBatch() {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║  RETRYING FAILED PAYMENTS BATCH                            ║");
        log.info("╚════════════════════════════════════════════════════════════╝");

        try {
            List<InstallmentPaymentEntity> failedPayments = installmentService.getOverduePayments();

            List<InstallmentPaymentEntity> retriablePayments = failedPayments.stream()
                    .filter(InstallmentPaymentEntity::canRetry)
                    .filter(p -> p.getPaymentStatus() == PaymentStatus.FAILED ||
                            p.getPaymentStatus() == PaymentStatus.LATE)
                    .toList();

            log.info("Found {} failed payments", failedPayments.size());
            log.info("Retriable payments: {}", retriablePayments.size());

            if (retriablePayments.isEmpty()) {
                log.info("No payments to retry");
                return;
            }

            int successCount = 0;
            int failedCount = 0;

            for (InstallmentPaymentEntity payment : retriablePayments) {
                try {
                    jobScheduler.schedule(
                            Instant.now().plus(5, ChronoUnit.MINUTES),
                            () -> paymentProcessor.retryPayment(payment.getPaymentId())
                    );
                    successCount++;

                    log.debug("Scheduled retry for payment: {} - Retry count: {}",
                            payment.getPaymentId(),
                            payment.getRetryCount());

                } catch (Exception e) {
                    failedCount++;
                    log.error("Failed to schedule retry for payment: {}",
                            payment.getPaymentId(), e);
                }
            }

            log.info("Retry batch summary:");
            log.info("  Total retriable: {}", retriablePayments.size());
            log.info("  Successfully scheduled: {}", successCount);
            log.info("  Failed to schedule: {}", failedCount);
            log.info("╚════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            log.error("Error in retryFailedPaymentsBatch", e);
            throw new RuntimeException("Failed to retry failed payments batch", e);
        }
    }

    public void markOverduePaymentsBatch() {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║  MARKING OVERDUE PAYMENTS BATCH                            ║");
        log.info("╚════════════════════════════════════════════════════════════╝");

        try {
            List<InstallmentPaymentEntity> overduePayments = installmentService.getOverduePayments();

            List<InstallmentPaymentEntity> unmarkedPayments = overduePayments.stream()
                    .filter(p -> p.getPaymentStatus() != PaymentStatus.LATE &&
                            p.getPaymentStatus() != PaymentStatus.SKIPPED)
                    .filter(p -> !p.isPaid())
                    .toList();

            log.info("Found {} overdue payments", overduePayments.size());
            log.info("Unmarked overdue payments: {}", unmarkedPayments.size());

            if (unmarkedPayments.isEmpty()) {
                log.info("No payments to mark as overdue");
                return;
            }

            int successCount = 0;
            int failedCount = 0;

            for (InstallmentPaymentEntity payment : unmarkedPayments) {
                try {
                    jobScheduler.enqueue(() ->
                            paymentProcessor.markPaymentOverdue(payment.getPaymentId())
                    );
                    successCount++;

                    log.debug("Enqueued overdue marking for payment: {} - Days overdue: {}",
                            payment.getPaymentId(),
                            payment.getDaysOverdue());

                } catch (Exception e) {
                    failedCount++;
                    log.error("Failed to enqueue overdue marking for payment: {}",
                            payment.getPaymentId(), e);
                }
            }

            log.info("Overdue batch summary:");
            log.info("  Total overdue: {}", unmarkedPayments.size());
            log.info("  Successfully enqueued: {}", successCount);
            log.info("  Failed to enqueue: {}", failedCount);
            log.info("╚════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            log.error("Error in markOverduePaymentsBatch", e);
            throw new RuntimeException("Failed to mark overdue payments batch", e);
        }
    }

    public void sendPaymentRemindersBatch() {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║  SENDING PAYMENT REMINDERS BATCH                           ║");
        log.info("╚════════════════════════════════════════════════════════════╝");

        try {
            List<InstallmentPaymentEntity> upcomingPayments = installmentService.getPaymentsDueToday();

            List<InstallmentPaymentEntity> reminderPayments = upcomingPayments.stream()
                    .filter(p -> p.getPaymentStatus() == PaymentStatus.SCHEDULED ||
                            p.getPaymentStatus() == PaymentStatus.PENDING)
                    .filter(p -> !p.isPaid())
                    .toList();

            log.info("Found {} upcoming payments", upcomingPayments.size());
            log.info("Reminder candidates: {}", reminderPayments.size());

            if (reminderPayments.isEmpty()) {
                log.info("No reminders to send");
                return;
            }

            int successCount = 0;
            int failedCount = 0;

            for (InstallmentPaymentEntity payment : reminderPayments) {
                try {
                    jobScheduler.enqueue(() ->
                            paymentProcessor.sendPaymentReminder(payment.getPaymentId())
                    );
                    successCount++;

                    log.debug("Enqueued reminder for payment: {} - Due: {}",
                            payment.getPaymentId(),
                            payment.getDueDate());

                } catch (Exception e) {
                    failedCount++;
                    log.error("Failed to enqueue reminder for payment: {}",
                            payment.getPaymentId(), e);
                }
            }

            log.info("Reminder batch summary:");
            log.info("  Total reminders: {}", reminderPayments.size());
            log.info("  Successfully enqueued: {}", successCount);
            log.info("  Failed to enqueue: {}", failedCount);
            log.info("╚════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            log.error("Error in sendPaymentRemindersBatch", e);
            throw new RuntimeException("Failed to send payment reminders batch", e);
        }
    }
}