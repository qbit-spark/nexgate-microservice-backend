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
public class AttendeeListResponse {

    private UUID eventId;
    private String eventTitle;
    private Integer dayNumber;
    private String dayName;
    private LocalDate dayDate;
    private UUID ticketTypeId;
    private String ticketTypeName;

    private DaySummary summary;
    private List<AttendeeInfo> attendees;
    private PaginationInfo pagination;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DaySummary {
        private Integer totalTicketsForType;
        private Integer checkedInThisDay;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendeeInfo {
        private UUID ticketInstanceId;
        private String attendeeName;
        private String attendeeEmail;
        private String attendeePhone;
        private String ticketType;
        private String ticketSeries;
        private String bookingReference;
        private BigDecimal pricePaid;
        private ZonedDateTime checkInTime;
        private String checkInLocation;
        private String checkedInBy;
        private String scannerId;
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