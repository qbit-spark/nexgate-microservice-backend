package org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.events;

import lombok.Builder;
import lombok.Getter;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.entity.EventBookingOrderEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.springframework.context.ApplicationEvent;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class BookingCreatedEvent extends ApplicationEvent {

    private final EventBookingOrderEntity bookingOrder;
    private final EventEntity event;
    private final List<EventBookingOrderEntity.BookedTicket> allTickets;
    private final Boolean sendTicketsToAttendees;

    @Builder
    public BookingCreatedEvent(
            Object source,
            EventBookingOrderEntity bookingOrder,
            EventEntity event,
            List<EventBookingOrderEntity.BookedTicket> allTickets,
            Boolean sendTicketsToAttendees) {

        super(source != null ? source : bookingOrder);  // Default source to bookingOrder if null
        this.bookingOrder = bookingOrder;
        this.event = event;
        this.allTickets = allTickets;
        this.sendTicketsToAttendees = sendTicketsToAttendees;
    }

    // ========================================
    // HELPER METHODS FOR LISTENER
    // ========================================

    /**
     * Get the buyer (always receives notification)
     */
    public AccountEntity getBuyer() {
        return bookingOrder.getCustomer();
    }

    /**
     * Get all tickets for the buyer
     */
    public List<EventBookingOrderEntity.BookedTicket> getBuyerTickets() {
        String buyerEmail = bookingOrder.getCustomer().getEmail().toLowerCase();
        return allTickets.stream()
                .filter(ticket -> ticket.getAttendeeEmail().toLowerCase().equals(buyerEmail))
                .toList();
    }

    /**
     * Get tickets grouped by attendee email
     * Use this when sendTicketsToAttendees = true
     */
    public Map<String, List<EventBookingOrderEntity.BookedTicket>> getTicketsByAttendeeEmail() {
        return allTickets.stream()
                .collect(Collectors.groupingBy(
                        ticket -> ticket.getAttendeeEmail().toLowerCase(),
                        Collectors.toList()
                ));
    }

    /**
     * Get list of unique attendee emails (excluding buyer)
     */
    public List<String> getOtherAttendeeEmails() {
        String buyerEmail = bookingOrder.getCustomer().getEmail().toLowerCase();
        return allTickets.stream()
                .map(ticket -> ticket.getAttendeeEmail().toLowerCase())
                .filter(email -> !email.equals(buyerEmail))
                .distinct()
                .toList();
    }

    /**
     * Check if buyer should get all tickets or just their own
     */
    public boolean shouldBuyerGetAllTickets() {
        return !sendTicketsToAttendees;
    }

    /**
     * Get total ticket count
     */
    public int getTotalTickets() {
        return allTickets.size();
    }

    /**
     * Get buyer's ticket count only
     */
    public int getBuyerTicketCount() {
        return getBuyerTickets().size();
    }
}