package org.nextgate.nextgatebackend.notification_system.publisher.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationChannel;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationPriority;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationType;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    @JsonProperty("type")
    private NotificationType type;

    @JsonProperty("recipients")
    private List<Recipient> recipients;

    @JsonProperty("channels")
    private List<NotificationChannel> channels;

    @JsonProperty("priority")
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @JsonProperty("data")
    private Map<String, Object> data;
}