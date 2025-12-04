package org.nextgate.nextgatebackend.e_events.events_mng.analytics.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventPerformanceResponse {

    private UUID eventId;
    private String eventTitle;
    private LocalDateTime eventDate;
    private String status;

    private FinancialMetrics financials;
    private TicketMetrics ticketMetrics;
    private AttendanceMetrics attendanceMetrics;
    private EventTimeline timeline;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialMetrics {
        private BigDecimal totalRevenue;
        private BigDecimal inEscrow;
        private BigDecimal released;
        private BigDecimal refunded;
        private BigDecimal averageTicketPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketMetrics {
        private Boolean hasUnlimitedTickets;
        private Integer limitedCapacity;
        private Integer limitedSold;
        private Integer limitedRemaining;
        private Integer unlimitedSold;
        private Integer totalSold;
        private Integer displayCapacity;
        private Double sellOutPercentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceMetrics {
        private Integer totalTickets;
        private Integer checkedIn;
        private Integer noShows;
        private Double attendanceRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventTimeline {
        private LocalDateTime createdAt;
        private LocalDateTime publishedAt;
        private LocalDateTime firstSaleAt;
        private LocalDateTime eventDate;
        private LocalDateTime completedAt;
    }
}