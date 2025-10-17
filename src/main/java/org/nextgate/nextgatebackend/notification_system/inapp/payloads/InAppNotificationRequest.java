package org.nextgate.nextgatebackend.notification_system.inapp.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InAppNotificationRequest {

    @NotNull(message = "User ID is required")
    @JsonProperty("userId")
    private UUID userId;

    @JsonProperty("shopId")
    private UUID shopId;

    @NotBlank(message = "Service ID is required")
    @JsonProperty("serviceId")
    private String serviceId;

    @NotBlank(message = "Service type is required")
    @JsonProperty("serviceType")
    private String serviceType;

    @NotBlank(message = "Title is required")
    @JsonProperty("title")
    private String title;

    @NotBlank(message = "Message is required")
    @JsonProperty("message")
    private String message;

    @NotBlank(message = "Type is required")
    @JsonProperty("type")
    private String type;

    @NotBlank(message = "Priority is required")
    @JsonProperty("priority")
    private String priority;

    @JsonProperty("data")
    private Map<String, Object> data;
}