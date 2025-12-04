
package org.nextgate.nextgatebackend.e_commerce.installment_purchase.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.entity.InstallmentAgreementEntity;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.entity.InstallmentPaymentEntity;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.events.InstallmentAgreementCompletedEvent;
import org.nextgate.nextgatebackend.notification_system.publisher.NotificationPublisher;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.NotificationEvent;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.Recipient;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationChannel;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationPriority;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Simple installment notification handler - following group purchase pattern
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InstallmentNotificationListener {

    private final NotificationPublisher notificationPublisher;

    // ═══════════════════════════════════════════════════════════════════════════════════
    // 🎯 ASYNC EVENT LISTENERS (Background Notifications)
    // ═══════════════════════════════════════════════════════════════════════════════════

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onAgreementCompleted(InstallmentAgreementCompletedEvent event) {
        log.info("🏆 Agreement completed notification: {}", event.getAgreementId());

        try {
            InstallmentAgreementEntity agreement = event.getAgreement();

            // Simple data map - like group purchase
            Map<String, Object> data = Map.of(
                    "agreementNumber", agreement.getAgreementNumber(),
                    "customerName", agreement.getCustomer().getUserName(),
                    "totalAmount", agreement.getTotalAmount(),
                    "completedAt", agreement.getCompletedAt(),
                    "isEarlyPayoff", event.isEarlyPayoff()
            );

            // Simple recipient - like group purchase
            Recipient recipient = createRecipient(agreement);

            // Simple notification event - like group purchase
            NotificationEvent notification = NotificationEvent.builder()
                    .type(NotificationType.INSTALLMENT_AGREEMENT_COMPLETED)
                    .recipients(List.of(recipient))
                    .channels(List.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP))
                    .priority(NotificationPriority.NORMAL)
                    .data(data)
                    .build();

            notificationPublisher.publish(notification);

            log.info("✅ Agreement completion notification sent: {}",
                    agreement.getCustomer().getEmail());

        } catch (Exception e) {
            log.error("❌ Failed to send agreement completion notification", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // 🚀 DIRECT NOTIFICATION METHODS (Immediate Feedback)
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Send payment success notification - immediate feedback
     */
    public void sendPaymentSuccess(InstallmentPaymentEntity payment) {
        log.info("💰 Sending payment success notification");

        try {
            Map<String, Object> data = Map.of(
                    "agreementNumber", payment.getAgreement().getAgreementNumber(),
                    "paymentAmount", payment.getScheduledAmount(),
                    "paidAmount", payment.getPaidAmount(),
                    "paymentNumber", payment.getPaymentNumber(),
                    "paidAt", payment.getPaidAt()
            );

            Recipient recipient = createRecipient(payment.getAgreement());

            NotificationEvent notification = NotificationEvent.builder()
                    .type(NotificationType.INSTALLMENT_PAYMENT_SUCCESS)
                    .recipients(List.of(recipient))
                    .channels(List.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP))
                    .priority(NotificationPriority.NORMAL)
                    .data(data)
                    .build();

            notificationPublisher.publish(notification);
            log.info("✅ Payment success notification sent");

        } catch (Exception e) {
            log.error("❌ Failed to send payment success notification", e);
        }
    }

    /**
     * Send payment reminder - called from scheduled job
     */
    public void sendPaymentReminder(InstallmentPaymentEntity payment) {
        log.info("🔔 Sending payment reminder");

        try {
            Map<String, Object> data = Map.of(
                    "agreementNumber", payment.getAgreement().getAgreementNumber(),
                    "paymentAmount", payment.getScheduledAmount(),
                    "dueDate", payment.getDueDate(),
                    "paymentNumber", payment.getPaymentNumber(),
                    "paymentStatus", payment.getPaymentStatus().name()
            );

            Recipient recipient = createRecipient(payment.getAgreement());

            NotificationEvent notification = NotificationEvent.builder()
                    .type(NotificationType.INSTALLMENT_PAYMENT_REMINDER)
                    .recipients(List.of(recipient))
                    .channels(List.of(NotificationChannel.EMAIL, NotificationChannel.SMS, NotificationChannel.IN_APP))
                    .priority(NotificationPriority.NORMAL)
                    .data(data)
                    .build();

            notificationPublisher.publish(notification);
            log.info("✅ Payment reminder sent");

        } catch (Exception e) {
            log.error("❌ Failed to send payment reminder", e);
        }
    }

    /**
     * Send flexible payment success - immediate feedback
     */
    public void sendFlexiblePaymentSuccess(InstallmentPaymentEntity payment, BigDecimal extraAmount) {
        log.info("🌟 Sending flexible payment success notification");

        try {
            Map<String, Object> data = Map.of(
                    "agreementNumber", payment.getAgreement().getAgreementNumber(),
                    "scheduledAmount", payment.getScheduledAmount(),
                    "paidAmount", payment.getPaidAmount(),
                    "extraAmount", extraAmount,
                    "paidAt", payment.getPaidAt()
            );

            Recipient recipient = createRecipient(payment.getAgreement());

            NotificationEvent notification = NotificationEvent.builder()
                    .type(NotificationType.INSTALLMENT_FLEXIBLE_PAYMENT_SUCCESS)
                    .recipients(List.of(recipient))
                    .channels(List.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP))
                    .priority(NotificationPriority.NORMAL)
                    .data(data)
                    .build();

            notificationPublisher.publish(notification);
            log.info("✅ Flexible payment notification sent");

        } catch (Exception e) {
            log.error("❌ Failed to send flexible payment notification", e);
        }
    }

    /**
     * Send payment failure - immediate feedback
     */
    public void sendPaymentFailure(InstallmentPaymentEntity payment, String failureReason) {
        log.warn("💸 Sending payment failure notification");

        try {
            Map<String, Object> data = Map.of(
                    "agreementNumber", payment.getAgreement().getAgreementNumber(),
                    "paymentAmount", payment.getScheduledAmount(),
                    "dueDate", payment.getDueDate(),
                    "failureReason", failureReason,
                    "paymentStatus", payment.getPaymentStatus().name(),
                    "retryCount", payment.getRetryCount()
            );

            Recipient recipient = createRecipient(payment.getAgreement());

            NotificationEvent notification = NotificationEvent.builder()
                    .type(NotificationType.INSTALLMENT_PAYMENT_FAILED)
                    .recipients(List.of(recipient))
                    .channels(List.of(NotificationChannel.EMAIL, NotificationChannel.SMS, NotificationChannel.IN_APP))
                    .priority(NotificationPriority.HIGH)
                    .data(data)
                    .build();

            notificationPublisher.publish(notification);
            log.warn("✅ Payment failure notification sent");

        } catch (Exception e) {
            log.error("❌ Failed to send payment failure notification", e);
        }
    }

    /**
     * Send overdue payment warning - called from scheduled job
     */
    public void sendOverdueWarning(InstallmentPaymentEntity payment, int daysOverdue) {
        log.warn("⚠️ Sending overdue payment warning - {}d overdue", daysOverdue);

        try {
            Map<String, Object> data = Map.of(
                    "agreementNumber", payment.getAgreement().getAgreementNumber(),
                    "paymentAmount", payment.getScheduledAmount(),
                    "dueDate", payment.getDueDate(),
                    "daysOverdue", daysOverdue,
                    "lateFee", payment.getLateFee() != null ? payment.getLateFee() : BigDecimal.ZERO
            );

            Recipient recipient = createRecipient(payment.getAgreement());

            NotificationEvent notification = NotificationEvent.builder()
                    .type(NotificationType.INSTALLMENT_PAYMENT_OVERDUE)
                    .recipients(List.of(recipient))
                    .channels(List.of(NotificationChannel.EMAIL, NotificationChannel.SMS, NotificationChannel.IN_APP))
                    .priority(NotificationPriority.HIGH)
                    .data(data)
                    .build();

            notificationPublisher.publish(notification);
            log.warn("✅ Overdue warning sent - {}d overdue", daysOverdue);

        } catch (Exception e) {
            log.error("❌ Failed to send overdue warning", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // 🛠️ HELPER METHOD
    // ═══════════════════════════════════════════════════════════════════════════════════

    private Recipient createRecipient(InstallmentAgreementEntity agreement) {
        return Recipient.builder()
                .userId(agreement.getCustomer().getId().toString())
                .email(agreement.getCustomer().getEmail())
                .phone(agreement.getCustomer().getPhoneNumber())
                .name(agreement.getCustomer().getUserName())
                .language("en")
                .build();
    }
}