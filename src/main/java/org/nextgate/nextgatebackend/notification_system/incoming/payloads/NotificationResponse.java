package org.nextgate.nextgatebackend.notification_system.incoming.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("userId")
    private UUID userId;

    @JsonProperty("shopId")
    private UUID shopId;

    @JsonProperty("serviceId")
    private String serviceId;

    @JsonProperty("serviceType")
    private String serviceType;

    @JsonProperty("title")
    private String title;

    @JsonProperty("message")
    private String message;

    @JsonProperty("type")
    private String type;

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("isRead")
    private Boolean isRead;

    @JsonProperty("data")
    private Map<String, Object> data;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("readAt")
    private LocalDateTime readAt;
}