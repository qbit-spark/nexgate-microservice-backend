package org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.enums;

/**
 * Status of an individual ticket instance
 */
public enum TicketInstanceStatus {
    /**
     * Ticket is active and can be used for check-in
     */
    ACTIVE,

    /**
     * Ticket has been used (checked in at event)
     */
    USED,

    /**
     * Ticket has been cancelled (placeholder for future)
     */
    CANCELLED
}