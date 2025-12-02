package org.nextgate.nextgatebackend.e_events.events_mng.analytics.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventRevenueResponse {

    private List<EventRevenue> events;
    private PaginationInfo pagination;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventRevenue {
        private UUID eventId;
        private String eventTitle;
        private LocalDateTime eventDate;
        private String status;

        private Integer ticketsSold;
        private BigDecimal totalRevenue;
        private BigDecimal inEscrow;
        private BigDecimal released;
        private BigDecimal refunded;

        private Double attendanceRate;
        private Integer totalCapacity;
        private Double sellOutPercentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationInfo {
        private Integer currentPage;
        private Integer pageSize;
        private Integer totalPages;
        private Long totalElements;
        private Boolean hasNext;
        private Boolean hasPrevious;
    }
}