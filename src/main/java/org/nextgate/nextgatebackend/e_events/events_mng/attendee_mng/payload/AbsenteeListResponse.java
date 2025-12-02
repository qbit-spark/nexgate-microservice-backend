package org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AbsenteeListResponse {

    private UUID eventId;
    private String eventTitle;
    private Integer dayNumber;
    private String dayName;
    private LocalDate dayDate;
    private UUID ticketTypeId;
    private String ticketTypeName;

    private AbsenteeSummary summary;
    private List<AbsenteeInfo> absentees;
    private AttendeeListResponse.PaginationInfo pagination;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbsenteeSummary {
        private Integer totalTicketsForType;
        private Integer absentThisDay;
        private Double absenteeRate;
        private AbsenteeBreakdown breakdown;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbsenteeBreakdown {
        private Integer fullNoShow;
        private Integer specificDayOnly;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbsenteeInfo {
        private UUID ticketInstanceId;
        private String attendeeName;
        private String attendeeEmail;
        private String attendeePhone;
        private String ticketType;
        private String ticketSeries;
        private String bookingReference;
        private BigDecimal pricePaid;
        private String statusForThisDay;
        private AttendancePattern attendancePattern;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendancePattern {
        private Integer totalEventDays;
        private Integer daysAttended;
        private Integer daysAbsent;
        private List<Integer> attendedDayNumbers;
        private List<Integer> absentDayNumbers;
        private String category;
    }
}