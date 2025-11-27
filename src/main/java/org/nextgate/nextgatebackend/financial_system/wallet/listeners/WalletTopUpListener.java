package org.nextgate.nextgatebackend.financial_system.wallet.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.financial_system.wallet.events.WalletTopUpEvent;
import org.nextgate.nextgatebackend.notification_system.publisher.NotificationPublisher;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.NotificationEvent;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.Recipient;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationChannel;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationPriority;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationType;
import org.nextgate.nextgatebackend.notification_system.publisher.mapper.WalletNotificationMapper;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class WalletTopUpListener {

    private final NotificationPublisher notificationPublisher;

    @Async("notificationExecutor")
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWalletTopUp(WalletTopUpEvent event) {

        long startTime = System.currentTimeMillis();

        try {
            log.info("📧 Processing top-up notification for: {}",
                    event.getAccount().getUserName());

            sendTopUpNotification(
                    event.getAccount(),
                    event.getAmount(),
                    event.getNewBalance(),
                    event.getTransactionRef()
            );

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Notification sent successfully in {}ms", duration);

        } catch (Exception e) {
            log.error("❌ Failed to send notification after {}ms: {}",
                    System.currentTimeMillis() - startTime, e.getMessage());
        }
    }

    private void sendTopUpNotification(
            AccountEntity customer,
            BigDecimal topUpAmount,
            BigDecimal newBalance,
            String transactionId) {

        // 1. Prepare notification data using EventCategoryMapper
        Map<String, Object> data = WalletNotificationMapper.mapWalletTopUp(
                customer.getFirstName(),
                topUpAmount,
                newBalance,
                transactionId
        );

        // 2. Build recipient
        Recipient recipient = Recipient.builder()
                .userId(customer.getId().toString())
                .email(customer.getEmail())
                .phone(customer.getPhoneNumber())
                .name(customer.getFirstName())
                .language("en")  // Default language
                .build();

        // 3. Create notification event
        NotificationEvent event = NotificationEvent.builder()
                .type(NotificationType.WALLET_BALANCE_UPDATE)
                .recipients(List.of(recipient))
                .channels(List.of(
                        NotificationChannel.EMAIL,
                        NotificationChannel.SMS,
                        NotificationChannel.PUSH,
                        NotificationChannel.IN_APP
                ))
                .priority(NotificationPriority.NORMAL)
                .data(data)
                .build();

        // 4. Publish notification
        notificationPublisher.publish(event);

        log.info("📤 Wallet top-up notification sent: user={}, amount={}, txn={}",
                customer.getUserName(), topUpAmount, transactionId);

    }
}