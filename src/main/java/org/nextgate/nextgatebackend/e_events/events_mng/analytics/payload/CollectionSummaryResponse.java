package org.nextgate.nextgatebackend.e_events.events_mng.analytics.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CollectionSummaryResponse {

    private EventMetrics eventMetrics;
    private CollectionMetrics collectionMetrics;
    private TopPerformer topEvent;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventMetrics {
        private Integer totalEvents;
        private Integer upcomingEvents;
        private Integer ongoingEvents;
        private Integer completedEvents;
        private Integer cancelledEvents;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionMetrics {
        private Integer totalTicketsSold;
        private BigDecimal totalRevenue;
        private BigDecimal inEscrow;
        private BigDecimal released;
        private BigDecimal refunded;
        private BigDecimal pendingRefunds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopPerformer {
        private UUID eventId;
        private String eventTitle;
        private BigDecimal revenue;
        private Integer ticketsSold;
        private Double attendanceRate;
    }
}