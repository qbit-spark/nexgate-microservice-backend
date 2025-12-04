package org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingOrderSummaryResponse {

    private UUID bookingId;
    private String bookingReference;
    private BookingStatus status;

    private String eventTitle;
    private LocalDateTime eventStartDateTime;
    private String eventLocation;

    private Integer totalTickets;
    private Integer checkedInTickets;

    private BigDecimal total;

    private LocalDateTime bookedAt;
}