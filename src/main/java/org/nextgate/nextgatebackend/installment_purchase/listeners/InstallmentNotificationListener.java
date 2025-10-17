package org.nextgate.nextgatebackend.installment_purchase.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.installment_purchase.events.InstallmentAgreementCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for InstallmentAgreementCompletedEvent and sends notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InstallmentNotificationListener {

    // private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // ← ADD THIS
    @Async
    public void onAgreementCompleted(InstallmentAgreementCompletedEvent event) {

        log.info("Sending agreement completion notification to: {}",
                event.getAgreement().getCustomer().getUserName());

        try {
            String message = event.isEarlyPayoff()
                    ? "Congratulations on paying off early! Your order is being processed."
                    : "Congratulations on completing your payment plan! Your order is being processed.";

            // TODO: Send notification
            // notificationService.sendAgreementCompletionNotification(
            //     event.getAgreement().getCustomer(),
            //     event.getAgreement(),
            //     message
            // );

            log.info("✓ Notification sent to: {}",
                    event.getAgreement().getCustomer().getEmail());

        } catch (Exception e) {
            log.error("Failed to send agreement completion notification", e);
        }
    }
}