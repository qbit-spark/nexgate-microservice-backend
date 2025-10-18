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
public class NotificationSummaryResponse {

    @JsonProperty("totalCount")
    private Long totalCount;

    @JsonProperty("unreadCount")
    private Long unreadCount;

    @JsonProperty("readCount")
    private Long readCount;
}