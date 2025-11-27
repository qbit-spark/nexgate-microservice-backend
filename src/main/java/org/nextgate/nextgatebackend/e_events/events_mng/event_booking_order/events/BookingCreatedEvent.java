package org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingCreatedEvent {

    private UUID bookingId;
    private UUID customerId;
    private UUID eventId;
    private Boolean sendTicketsToAttendees;
}