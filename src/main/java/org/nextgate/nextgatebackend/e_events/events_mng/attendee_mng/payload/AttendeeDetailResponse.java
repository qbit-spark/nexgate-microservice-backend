package org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendeeDetailResponse {

    private UUID ticketInstanceId;
    private String attendeeName;
    private String attendeeEmail;
    private String attendeePhone;
    private String ticketType;
    private String ticketSeries;
    private String bookingReference;
    private BigDecimal pricePaid;
    private String overallStatus;
    private Integer daysAttended;
    private Integer daysTotal;
    private List<DayCheckInInfo> checkInsByDay;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayCheckInInfo {
        private Integer dayNumber;
        private String dayName;
        private LocalDate dayDate;
        private String status;
        private ZonedDateTime checkInTime;
        private String checkInLocation;
        private String checkedInBy;
        private String scannerId;
    }
}