package org.nextgate.nextgatebackend.e_events.events_mng.analytics.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RevenueTrendResponse {

    private String period;
    private Integer totalEvents;
    private List<PeriodData> trends;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodData {
        private String label;
        private Integer year;
        private Integer month;

        private Integer eventsCount;
        private Integer ticketsSold;
        private BigDecimal revenue;
        private BigDecimal inEscrow;
        private BigDecimal released;

        private Double averageAttendanceRate;
        private Double averageSellOutRate;
    }
}