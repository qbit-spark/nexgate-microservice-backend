package org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.events.PaymentSuccessNotificationEvent;
import org.nextgate.nextgatebackend.notification_system.publisher.NotificationPublisher;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.NotificationEvent;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.Recipient;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationChannel;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationPriority;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationType;
import org.nextgate.nextgatebackend.notification_system.publisher.mapper.PaymentNotificationMapper;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSuccessNotificationListener {

    private final NotificationPublisher notificationPublisher;

    @EventListener
    @Async
    public void onPaymentSuccess(PaymentSuccessNotificationEvent event) {

        log.info("📧 Processing payment success notification | Session: {}", event.getSessionId());

        try {
            Map<String, Object> data = PaymentNotificationMapper.mapPaymentReceived(
                    event.getCustomer().getFirstName(),
                    event.getCustomer().getEmail(),
                    event.getEscrow().getTotalAmount(),
                    event.getEscrow().getCurrency(),
                    "WALLET",
                    event.getEscrow().getId().toString(),
                    event.getEscrow().getEscrowNumber(),
                    event.getSessionId().toString()
            );

            Recipient recipient = Recipient.builder()
                    .userId(event.getCustomer().getId().toString())
                    .email(event.getCustomer().getEmail())
                    .phone(event.getCustomer().getPhoneNumber())
                    .name(event.getCustomer().getFirstName())
                    .language("en")
                    .build();

            NotificationEvent notificationEvent = NotificationEvent.builder()
                    .type(NotificationType.PAYMENT_RECEIVED)
                    .recipients(List.of(recipient))
                    .channels(List.of(
                            NotificationChannel.EMAIL,
                            NotificationChannel.SMS,
                            NotificationChannel.PUSH,
                            NotificationChannel.IN_APP
                    ))
                    .priority(NotificationPriority.HIGH)
                    .data(data)
                    .build();

            notificationPublisher.publish(notificationEvent);

            log.info("✓ Payment success notification sent | Session: {}", event.getSessionId());

        } catch (Exception e) {
            log.error("Failed to send payment notification | Session: {}", event.getSessionId(), e);
            // Don't propagate - notifications are non-critical
        }
    }
}