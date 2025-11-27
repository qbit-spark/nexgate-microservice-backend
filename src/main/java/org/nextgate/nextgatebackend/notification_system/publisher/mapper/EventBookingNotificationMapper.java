package org.nextgate.nextgatebackend.notification_system.publisher.mapper;


import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventBookingNotificationMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    /**
     * Map data for buyer's booking confirmation
     */
    public static Map<String, Object> mapBookingConfirmation(
            String buyerName,
            String buyerEmail,
            String bookingReference,
            String eventTitle,
            String eventLocation,
            ZonedDateTime eventStartDate,
            int totalTickets,
            int buyerTickets,
            BigDecimal totalAmount,
            List<String> qrCodes) {

        Map<String, Object> data = new HashMap<>();

        // Buyer info
        data.put("buyerName", buyerName);
        data.put("buyerEmail", buyerEmail);

        // Booking info
        data.put("bookingReference", bookingReference);
        data.put("totalTickets", totalTickets);
        data.put("buyerTickets", buyerTickets);
        data.put("totalAmount", totalAmount);
        data.put("currency", "TZS");

        // Event info
        data.put("eventTitle", eventTitle);
        data.put("eventLocation", eventLocation);
        data.put("eventDate", eventStartDate.format(DATE_FORMATTER));
        data.put("eventTime", eventStartDate.format(TIME_FORMATTER));
        data.put("eventDateTime", eventStartDate);

        // QR codes
        data.put("qrCodes", qrCodes);
        data.put("qrCodesCount", qrCodes.size());

        return data;
    }

    /**
     * Map data for attendee's ticket received notification
     */
    public static Map<String, Object> mapTicketReceived(
            String attendeeName,
            String attendeeEmail,
            String bookingReference,
            String eventTitle,
            String eventLocation,
            ZonedDateTime eventStartDate,
            int ticketCount,
            List<String> qrCodes,
            String buyerName) {

        Map<String, Object> data = new HashMap<>();

        // Attendee info
        data.put("attendeeName", attendeeName);
        data.put("attendeeEmail", attendeeEmail);

        // Booking info
        data.put("bookingReference", bookingReference);
        data.put("ticketCount", ticketCount);
        data.put("buyerName", buyerName);  // Who bought the tickets

        // Event info
        data.put("eventTitle", eventTitle);
        data.put("eventLocation", eventLocation);
        data.put("eventDate", eventStartDate.format(DATE_FORMATTER));
        data.put("eventTime", eventStartDate.format(TIME_FORMATTER));
        data.put("eventDateTime", eventStartDate);

        // QR codes
        data.put("qrCodes", qrCodes);
        data.put("qrCodesCount", qrCodes.size());

        return data;
    }

    /**
     * Map data for organizer's new booking notification
     */
    public static Map<String, Object> mapOrganizerNewBooking(
            String organizerName,
            String organizerEmail,
            String bookingReference,
            String eventTitle,
            String buyerName,
            String buyerEmail,
            int totalTickets,
            BigDecimal totalAmount,
            ZonedDateTime bookingDate) {

        Map<String, Object> data = new HashMap<>();

        // Organizer info
        data.put("organizerName", organizerName);
        data.put("organizerEmail", organizerEmail);

        // Booking info
        data.put("bookingReference", bookingReference);
        data.put("totalTickets", totalTickets);
        data.put("totalAmount", totalAmount);
        data.put("currency", "TZS");
        data.put("bookingDate", bookingDate.format(DATE_FORMATTER));
        data.put("bookingTime", bookingDate.format(TIME_FORMATTER));

        // Buyer info
        data.put("buyerName", buyerName);
        data.put("buyerEmail", buyerEmail);

        // Event info
        data.put("eventTitle", eventTitle);

        return data;
    }
}