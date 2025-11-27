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
    private Integer checkedInTickets;

    private BigDecimal subtotal;
    private BigDecimal total;

    private LocalDateTime bookedAt;
    private LocalDateTime cancelledAt;

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
    public static class BookedTicketResponse {
        private UUID ticketInstanceId;
        private String ticketTypeName;
        private String ticketSeries;
        private BigDecimal price;
        private String qrCode;
        private String attendanceMode;

        private AttendeeInfo attendee;
        private BuyerInfo buyer;

        private Boolean checkedIn;
        private ZonedDateTime checkedInAt;
        private String checkedInBy;
        private String checkInLocation;

        private TicketInstanceStatus status;
        private ZonedDateTime validFrom;
        private ZonedDateTime validUntil;
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
}