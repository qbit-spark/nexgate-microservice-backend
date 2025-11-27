package org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.entity.EventBookingOrderEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.events.BookingCreatedEvent;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.notification_system.publisher.NotificationPublisher;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.NotificationEvent;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.Recipient;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationChannel;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationPriority;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationType;
import org.nextgate.nextgatebackend.notification_system.publisher.mapper.EventBookingNotificationMapper;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingNotificationListener {

    private final NotificationPublisher notificationPublisher;

    @EventListener
    @Async
    public void onBookingCreated(BookingCreatedEvent event) {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║   BOOKING NOTIFICATION LISTENER                            ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("Booking: {} | Total Tickets: {} | SendToAttendees: {}",
                event.getBookingOrder().getBookingReference(),
                event.getTotalTickets(),
                event.getSendTicketsToAttendees());

        try {
            // 1. Always send confirmation to buyer
            sendBuyerConfirmation(event);

            // 2. Conditionally send to other attendees
            if (event.getSendTicketsToAttendees()) {
                sendAttendeeNotifications(event);
            }

            // 3. Always notify event organizer
            sendOrganizerNotification(event);

            log.info("✓ All booking notifications sent successfully");

        } catch (Exception e) {
            log.error("❌ Failed to send booking notifications", e);
        }
    }

    /**
     * Send booking confirmation to buyer (ALWAYS)
     */
    private void sendBuyerConfirmation(BookingCreatedEvent event) {
        AccountEntity buyer = event.getBuyer();
        EventBookingOrderEntity booking = event.getBookingOrder();
        EventEntity eventEntity = event.getEvent();

        // Determine which tickets to attach
        List<EventBookingOrderEntity.BookedTicket> ticketsToAttach;
        String notificationMessage;

        if (event.shouldBuyerGetAllTickets()) {
            // Buyer gets ALL tickets (for entire group)
            ticketsToAttach = event.getAllTickets();
            notificationMessage = String.format(
                    "Your booking is confirmed! You have %d ticket(s) total for your group. " +
                            "Please distribute tickets to your attendees.",
                    event.getTotalTickets()
            );
        } else {
            // Buyer gets only their own tickets
            ticketsToAttach = event.getBuyerTickets();
            notificationMessage = String.format(
                    "Your booking is confirmed! You have %d ticket(s). " +
                            "Other attendees will receive their tickets separately.",
                    event.getBuyerTicketCount()
            );
        }

        // Extract QR codes
        List<String> qrCodes = ticketsToAttach.stream()
                .map(EventBookingOrderEntity.BookedTicket::getQrCode)
                .collect(Collectors.toList());

        // Prepare notification data
        Map<String, Object> data = EventBookingNotificationMapper.mapBookingConfirmation(
                buyer.getFirstName() + " " + buyer.getLastName(),
                buyer.getEmail(),
                booking.getBookingReference(),
                eventEntity.getTitle(),
                eventEntity.getVenue().getName(),
                eventEntity.getStartDateTime(),
                event.getTotalTickets(),
                event.getBuyerTicketCount(),
                booking.getTotal(),
                qrCodes
        );

        // Add custom message
        data.put("customMessage", notificationMessage);

        // Build recipient
        Recipient recipient = Recipient.builder()
                .userId(buyer.getId().toString())
                .email(buyer.getEmail())
                .phone(buyer.getPhoneNumber())
                .name(buyer.getFirstName())
                .language("en")
                .build();

        // Create notification event
        NotificationEvent notificationEvent = NotificationEvent.builder()
                .type(NotificationType.EVENT_BOOKING_CONFIRMED)
                .recipients(List.of(recipient))
                .channels(List.of(
                        NotificationChannel.EMAIL,
                        NotificationChannel.SMS,
                        NotificationChannel.PUSH,
                        NotificationChannel.IN_APP
                ))
                .priority(NotificationPriority.HIGH)
                .data(data)
                .build();

        // Send notification
        notificationPublisher.publish(notificationEvent);

        log.info("✓ Buyer confirmation sent | Email: {} | Tickets attached: {}",
                buyer.getEmail(), qrCodes.size());
    }

    /**
     * Send ticket notifications to other attendees (CONDITIONAL)
     */
    private void sendAttendeeNotifications(BookingCreatedEvent event) {
        Map<String, List<EventBookingOrderEntity.BookedTicket>> ticketsByEmail = event.getTicketsByAttendeeEmail();
        String buyerEmail = event.getBuyer().getEmail().toLowerCase();
        String buyerName = event.getBuyer().getFirstName() + " " + event.getBuyer().getLastName();
        EventEntity eventEntity = event.getEvent();
        EventBookingOrderEntity booking = event.getBookingOrder();

        int attendeeCount = 0;

        // Send to each attendee (excluding buyer)
        for (Map.Entry<String, List<EventBookingOrderEntity.BookedTicket>> entry : ticketsByEmail.entrySet()) {
            String attendeeEmail = entry.getKey();

            if (attendeeEmail.equals(buyerEmail)) {
                continue;  // Skip buyer (already notified)
            }

            List<EventBookingOrderEntity.BookedTicket> attendeeTickets = entry.getValue();
            EventBookingOrderEntity.BookedTicket firstTicket = attendeeTickets.getFirst();

            // Extract QR codes for this attendee
            List<String> qrCodes = attendeeTickets.stream()
                    .map(EventBookingOrderEntity.BookedTicket::getQrCode)
                    .collect(Collectors.toList());

            // Prepare notification data
            Map<String, Object> data = EventBookingNotificationMapper.mapTicketReceived(
                    firstTicket.getAttendeeName(),
                    attendeeEmail,
                    booking.getBookingReference(),
                    eventEntity.getTitle(),
                    eventEntity.getVenue().getName(),
                    eventEntity.getStartDateTime(),
                    attendeeTickets.size(),
                    qrCodes,
                    buyerName
            );

            // Build recipient
            Recipient recipient = Recipient.builder()
                    .email(attendeeEmail)
                    .phone(firstTicket.getAttendeePhone())
                    .name(firstTicket.getAttendeeName())
                    .language("en")
                    .build();

            // Create a notification event
            NotificationEvent notificationEvent = NotificationEvent.builder()
                    .type(NotificationType.EVENT_TICKET_RECEIVED)
                    .recipients(List.of(recipient))
                    .channels(List.of(
                            NotificationChannel.EMAIL,
                            NotificationChannel.SMS
                    ))
                    .priority(NotificationPriority.HIGH)
                    .data(data)
                    .build();

            // Send notification
            notificationPublisher.publish(notificationEvent);

            attendeeCount++;
            log.info("✓ Attendee notification sent | Email: {} | Tickets: {}",
                    attendeeEmail, attendeeTickets.size());
        }

        log.info("✓ Sent notifications to {} other attendees", attendeeCount);
    }

    /**
     * Send new booking notification to event organizer (ALWAYS)
     */
    private void sendOrganizerNotification(BookingCreatedEvent event) {
        EventEntity eventEntity = event.getEvent();
        AccountEntity organizer = eventEntity.getOrganizer();
        EventBookingOrderEntity booking = event.getBookingOrder();
        AccountEntity buyer = event.getBuyer();

        // Prepare notification data
        Map<String, Object> data = EventBookingNotificationMapper.mapOrganizerNewBooking(
                organizer.getFirstName() + " " + organizer.getLastName(),
                organizer.getEmail(),
                booking.getBookingReference(),
                eventEntity.getTitle(),
                buyer.getFirstName() + " " + buyer.getLastName(),
                buyer.getEmail(),
                event.getTotalTickets(),
                booking.getTotal(),
                booking.getBookedAt().atZone(eventEntity.getStartDateTime().getZone())
        );

        // Build recipient
        Recipient recipient = Recipient.builder()
                .userId(organizer.getId().toString())
                .email(organizer.getEmail())
                .phone(organizer.getPhoneNumber())
                .name(organizer.getFirstName())
                .language("en")
                .build();

        // Create a notification event
        NotificationEvent notificationEvent = NotificationEvent.builder()
                .type(NotificationType.EVENT_ORGANIZER_NEW_BOOKING)
                .recipients(List.of(recipient))
                .channels(List.of(
                        NotificationChannel.EMAIL,
                        NotificationChannel.IN_APP
                ))
                .priority(NotificationPriority.NORMAL)
                .data(data)
                .build();

        // Send notification
        notificationPublisher.publish(notificationEvent);

        log.info("✓ Organizer notification sent | Email: {} | Booking: {}",
                organizer.getEmail(), booking.getBookingReference());
    }
}