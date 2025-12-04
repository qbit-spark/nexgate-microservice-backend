package org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.utils.mapper;

import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.entity.EventBookingOrderEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.payload.BookingOrderResponse;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.payload.BookingOrderSummaryResponse;

import java.util.List;
import java.util.stream.Collectors;

public class BookingOrderMapper {

    public static BookingOrderResponse toResponse(EventBookingOrderEntity entity) {
        if (entity == null) {
            return null;
        }

        return BookingOrderResponse.builder()
                .bookingId(entity.getBookingId())
                .bookingReference(entity.getBookingReference())
                .status(entity.getStatus())
                .event(mapEventSnapshot(entity))
                .organizer(mapOrganizerSnapshot(entity))
                .customer(mapCustomerInfo(entity))
                .tickets(entity.getBookedTickets().stream()
                        .map(BookingOrderMapper::mapTicket)
                        .collect(Collectors.toList()))
                .totalTickets(entity.getTotalTicketCount())
                .checkedInTicketsCount((int) entity.getCheckedInCount())
                .subtotal(entity.getSubtotal())
                .total(entity.getTotal())
                .bookedAt(entity.getBookedAt())
                .cancelledAt(entity.getCancelledAt())
                .build();
    }

    public static BookingOrderSummaryResponse toSummaryResponse(EventBookingOrderEntity entity) {
        if (entity == null) {
            return null;
        }

        return BookingOrderSummaryResponse.builder()
                .bookingId(entity.getBookingId())
                .bookingReference(entity.getBookingReference())
                .status(entity.getStatus())
                .eventTitle(entity.getEventTitle())
                .eventStartDateTime(entity.getEventStartDateTime())
                .eventLocation(entity.getEventLocation())
                .totalTickets(entity.getTotalTicketCount())
                .checkedInTickets((int) entity.getCheckedInCount())
                .total(entity.getTotal())
                .bookedAt(entity.getBookedAt())
                .build();
    }

    private static BookingOrderResponse.EventSnapshot mapEventSnapshot(EventBookingOrderEntity entity) {
        return BookingOrderResponse.EventSnapshot.builder()
                .eventId(entity.getEvent().getId())
                .title(entity.getEventTitle())
                .startDateTime(entity.getEventStartDateTime())
                .endDateTime(entity.getEventEndDateTime())
                .timezone(entity.getEventTimezone())
                .location(entity.getEventLocation())
                .format(entity.getEventFormat())
                .virtualDetails(entity.getVirtualDetails())
                .build();
    }

    private static BookingOrderResponse.OrganizerSnapshot mapOrganizerSnapshot(EventBookingOrderEntity entity) {
        return BookingOrderResponse.OrganizerSnapshot.builder()
                .name(entity.getOrganizerName())
                .email(entity.getOrganizerEmail())
                .phone(entity.getOrganizerPhone())
                .build();
    }

    private static BookingOrderResponse.CustomerInfo mapCustomerInfo(EventBookingOrderEntity entity) {
        return BookingOrderResponse.CustomerInfo.builder()
                .customerId(entity.getCustomer().getId())
                .name(entity.getCustomer().getUserName())
                .email(entity.getCustomer().getEmail())
                .build();
    }

    private static BookingOrderResponse.BookedTicketResponse mapTicket(
            EventBookingOrderEntity.BookedTicket ticket) {

        // Safely get check-ins list (never null)
        List<EventBookingOrderEntity.BookedTicket.CheckInRecord> checkInRecords =
                ticket.getCheckIns() != null ? ticket.getCheckIns() : List.of();

        boolean hasAnyCheckIn = !checkInRecords.isEmpty();

        // Get the most recent check-in (null if none)
        EventBookingOrderEntity.BookedTicket.CheckInRecord lastCheckIn = ticket.getLastCheckIn();

        // Map each CheckInRecord → DTO
        List<BookingOrderResponse.BookedTicketResponse.CheckInRecordDto> checkInDtos = checkInRecords.stream()
                .map(rec -> BookingOrderResponse.BookedTicketResponse.CheckInRecordDto.builder()
                        .checkInTime(rec.getCheckInTime())
                        .checkInLocation(rec.getCheckInLocation())
                        .checkedInBy(rec.getCheckedInBy())
                        .dayName(rec.getDayName())
                        .scannerId(rec.getScannerId())
                        .checkInMethod(rec.getCheckInMethod() != null ? rec.getCheckInMethod() : "QR_SCAN")
                        .build())
                // Sort newest first (optional, but nice for frontend)
                .sorted((a, b) -> b.getCheckInTime().compareTo(a.getCheckInTime()))
                .toList();

        return BookingOrderResponse.BookedTicketResponse.builder()
                .ticketInstanceId(ticket.getTicketInstanceId())
                .ticketTypeName(ticket.getTicketTypeName())
                .ticketSeries(ticket.getTicketSeries())
                .price(ticket.getPrice())
                .attendanceMode(ticket.getAttendanceMode())
                .qrCode(ticket.getQrCode())

                .attendee(BookingOrderResponse.AttendeeInfo.builder()
                        .name(ticket.getAttendeeName())
                        .email(ticket.getAttendeeEmail())
                        .phone(ticket.getAttendeePhone())
                        .build())
                .buyer(BookingOrderResponse.BuyerInfo.builder()
                        .name(ticket.getBuyerName())
                        .email(ticket.getBuyerEmail())
                        .build())

                // === Multi-day check-in mapping ===
                .checkIns(checkInDtos)
                .hasBeenCheckedIn(hasAnyCheckIn)
                .lastCheckedInAt(lastCheckIn != null ? lastCheckIn.getCheckInTime() : null)
                .lastCheckedInBy(lastCheckIn != null ? lastCheckIn.getCheckedInBy() : null)
                .lastCheckInLocation(lastCheckIn != null ? lastCheckIn.getCheckInLocation() : null)
                .lastCheckInDayName(lastCheckIn != null ? lastCheckIn.getDayName() : null)

                .status(ticket.getStatus())
                .validFrom(ticket.getValidFrom())
                .validUntil(ticket.getValidUntil())
                .build();
    }
}