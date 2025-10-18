package org.nextgate.nextgatebackend.notification_system.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.NotificationEvent;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationDomain;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Simple notification publisher - sends notification events to RabbitMQ
 * Uses NotificationDomain enum to automatically route to correct queue
 *
 * Usage Example:
 * <pre>
 * NotificationEvent event = NotificationEvent.builder()
 *     .type(NotificationType.ORDER_CONFIRMATION)
 *     .recipients(recipients)
 *     .channels(channels)
 *     .data(data)
 *     .build();
 *
 * notificationPublisher.publish(event);
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;
    private static final String EXCHANGE_NAME = "notification.exchange";

    /**
     * Publish a notification event to RabbitMQ
     * Routing key is automatically determined from notification type using NotificationDomain enum
     */
    public void publish(NotificationEvent event) {
        try {
            validateEvent(event);

            // Enum automatically maps type to routing key - no errors possible!
            String routingKey = NotificationDomain.fromNotificationType(event.getType())
                    .getRoutingKey();

            log.info("📤 Publishing notification: type={}, domain={}, recipients={}",
                    event.getType(), routingKey, event.getRecipients().size());

            rabbitTemplate.convertAndSend(EXCHANGE_NAME, routingKey, event);

            log.info("✅ Published successfully: {}", routingKey);

        } catch (Exception e) {
            log.error("❌ Failed to publish notification: type={}, error={}",
                    event.getType(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish notification: " + e.getMessage(), e);
        }
    }

    /**
     * Validate notification event before publishing
     */
    private void validateEvent(NotificationEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getType() == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        if (event.getRecipients() == null || event.getRecipients().isEmpty()) {
            throw new IllegalArgumentException("Recipients cannot be empty");
        }
        if (event.getChannels() == null || event.getChannels().isEmpty()) {
            throw new IllegalArgumentException("Channels cannot be empty");
        }
    }
}