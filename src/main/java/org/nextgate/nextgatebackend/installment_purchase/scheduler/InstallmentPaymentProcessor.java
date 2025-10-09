package org.nextgate.nextgatebackend.installment_purchase.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.jobrunr.jobs.annotations.Job;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentAgreementEntity;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentPaymentEntity;
import org.nextgate.nextgatebackend.installment_purchase.repo.InstallmentPaymentRepo;
import org.nextgate.nextgatebackend.installment_purchase.service.InstallmentService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InstallmentPaymentProcessor {

    private final InstallmentService installmentService;
    private final InstallmentPaymentRepo paymentRepo;

    @Job(name = "Process Installment Payment", retries = 2)
    public void processPayment(UUID paymentId) {

        log.info("Processing payment: {}", paymentId);

        try {
            InstallmentPaymentEntity payment = installmentService.processInstallmentPayment(paymentId);

            log.info("Payment processed successfully: {} - Status: {}",
                    paymentId, payment.getPaymentStatus());

        } catch (ItemNotFoundException e) {
            log.error("Payment not found: {}", paymentId);
            throw new RuntimeException("Payment not found: " + paymentId, e);

        } catch (BadRequestException e) {
            log.error("Payment processing failed (business logic): {} - Reason: {}",
                    paymentId, e.getMessage());
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Unexpected error processing payment: {}", paymentId, e);
            throw new RuntimeException("Failed to process payment: " + paymentId, e);
        }
    }

    @Job(name = "Retry Failed Payment", retries = 3)
    public void retryPayment(UUID paymentId) throws ItemNotFoundException {

        log.info("Retrying failed payment: {}", paymentId);

        try {
            InstallmentPaymentEntity payment = paymentRepo.findById(paymentId)
                    .orElseThrow(() -> new ItemNotFoundException("Payment not found"));

            if (!payment.canRetry()) {
                log.warn("Payment cannot be retried: {} - Retry count: {}",
                        paymentId, payment.getRetryCount());
                return;
            }

            InstallmentPaymentEntity processedPayment =
                    installmentService.processInstallmentPayment(paymentId);

            log.info("Payment retry successful: {} - Status: {}",
                    paymentId, processedPayment.getPaymentStatus());

        } catch (Exception e) {
            log.error("Payment retry failed: {}", paymentId, e);
            installmentService.handleMissedPayment(paymentId, e.getMessage());
            throw new RuntimeException("Payment retry failed: " + paymentId, e);
        }
    }

    @Job(name = "Mark Payment as Overdue", retries = 1)
    public void markPaymentOverdue(UUID paymentId) {

        log.info("Marking payment as overdue: {}", paymentId);

        try {
            InstallmentPaymentEntity payment = paymentRepo.findById(paymentId)
                    .orElseThrow(() -> new ItemNotFoundException("Payment not found"));

            if (payment.isPaid()) {
                log.info("Payment already paid, skipping: {}", paymentId);
                return;
            }

            if (!payment.isOverdue()) {
                log.info("Payment not yet overdue, skipping: {}", paymentId);
                return;
            }

            payment.markAsLate();
            paymentRepo.save(payment);

            InstallmentAgreementEntity agreement = payment.getAgreement();
            agreement.recordMissedPayment();

            log.warn("Payment marked as overdue: {} - Days overdue: {}",
                    paymentId, payment.getDaysOverdue());

        } catch (Exception e) {
            log.error("Failed to mark payment as overdue: {}", paymentId, e);
            throw new RuntimeException("Failed to mark payment as overdue: " + paymentId, e);
        }
    }

    @Job(name = "Send Payment Reminder", retries = 2)
    public void sendPaymentReminder(UUID paymentId) {

        log.info("Sending payment reminder: {}", paymentId);

        try {
            InstallmentPaymentEntity payment = paymentRepo.findById(paymentId)
                    .orElseThrow(() -> new ItemNotFoundException("Payment not found"));

            if (payment.isPaid()) {
                log.info("Payment already paid, skipping reminder: {}", paymentId);
                return;
            }

            InstallmentAgreementEntity agreement = payment.getAgreement();

            log.info("Payment reminder sent for: {} - Customer: {} - Due: {}",
                    paymentId,
                    agreement.getCustomer().getEmail(),
                    payment.getDueDate());

        } catch (Exception e) {
            log.error("Failed to send payment reminder: {}", paymentId, e);
            throw new RuntimeException("Failed to send payment reminder: " + paymentId, e);
        }
    }
}