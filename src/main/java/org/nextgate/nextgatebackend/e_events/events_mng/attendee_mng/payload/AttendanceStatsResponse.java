package org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceStatsResponse {

    private UUID eventId;
    private String eventTitle;
    private Integer totalDays;
    private List<EventDaySchedule> eventSchedule;
    private OverallStats overallStats;
    private List<TicketTypeStats> byTicketType;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventDaySchedule {
        private Integer dayNumber;
        private String dayName;
        private LocalDate date;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverallStats {
        private Integer totalTickets;
        private Integer totalCheckedIn;
        private Integer totalAbsent;
        private Double attendanceRate;
        private List<DayStats> byDay;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayStats {
        private Integer dayNumber;
        private String dayName;
        private LocalDate date;
        private Integer totalTickets;
        private Integer checkedIn;
        private Integer absent;
        private Double attendanceRate;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketTypeStats {
        private UUID ticketTypeId;
        private String ticketTypeName;
        private Integer totalSold;
        private Integer totalCheckedIn;
        private Integer totalAbsent;
        private Double attendanceRate;
        private List<TicketTypeDayStats> byDay;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketTypeDayStats {
        private Integer dayNumber;
        private String dayName;
        private Integer checkedIn;
        private Integer absent;
        private Double attendanceRate;
    }
}