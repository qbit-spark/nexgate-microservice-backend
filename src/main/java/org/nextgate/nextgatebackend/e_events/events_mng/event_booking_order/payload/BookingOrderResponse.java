package org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.enums.BookingStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.enums.TicketInstanceStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.embedded.VirtualDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingOrderResponse {

    private UUID bookingId;
    private String bookingReference;
    private BookingStatus status;

    private EventSnapshot event;
    private OrganizerSnapshot organizer;
    private CustomerInfo customer;

    private List<BookedTicketResponse> tickets;
    private Integer totalTickets;
    private Integer checkedInTicketsCount; // Number of tickets with at least one check-in

    private BigDecimal subtotal;
    private BigDecimal total;

    private LocalDateTime bookedAt;
    private LocalDateTime cancelledAt;

    // =============================================================================
    // NESTED CLASSES
    // =============================================================================

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EventSnapshot {
        private UUID eventId;
        private String title;
        private LocalDateTime startDateTime;
        private LocalDateTime endDateTime;
        private String timezone;
        private String location;
        private String format;
        private VirtualDetails virtualDetails;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrganizerSnapshot {
        private String name;
        private String email;
        private String phone;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CustomerInfo {
        private UUID customerId;
        private String name;
        private String email;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttendeeInfo {
        private String name;
        private String email;
        private String phone;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BuyerInfo {
        private String name;
        private String email;
    }

    // =============================================================================
    // BOOKED TICKET RESPONSE – FULL MULTI-DAY SUPPORT
    // =============================================================================

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BookedTicketResponse {

        // Ticket Identification
        private UUID ticketInstanceId;
        private String ticketTypeName;
        private String ticketSeries;
        private BigDecimal price;
        private String qrCode;
        private String attendanceMode; // IN_PERSON, ONLINE

        // People
        private AttendeeInfo attendee;
        private BuyerInfo buyer;

        // === MULTI-DAY CHECK-IN SUPPORT ===
        private List<CheckInRecordDto> checkIns;           // Full history (Day 1, Day 2, etc.)

        // Convenience fields (for quick UI display & backward compatibility)
        private boolean hasBeenCheckedIn;                  // true if at least one check-in
        private ZonedDateTime lastCheckedInAt;             // Most recent check-in time
        private String lastCheckedInBy;
        private String lastCheckInLocation;
        private String lastCheckInDayName;                 // e.g., "Day 2 - Saturday"

        // Ticket Validity & Status
        private TicketInstanceStatus status;
        private ZonedDateTime validFrom;
        private ZonedDateTime validUntil;

        // Nested DTO for each check-in
        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class CheckInRecordDto {
            private ZonedDateTime checkInTime;
            private String checkInLocation;   // e.g., "Main Gate", "VIP Entrance"
            private String checkedInBy;       // Staff/scanner name
            private String dayName;           // Must match event schedule (e.g., "Day 1", "Friday")
            private String scannerId;
            private String checkInMethod;     // QR_SCAN, MANUAL, NFC, etc. (default: QR_SCAN)
        }
    }
}