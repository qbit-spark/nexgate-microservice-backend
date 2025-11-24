package org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums;

/**
 * Status of ticket type
 */
public enum TicketStatus {
    /**
     * Ticket is active and available for purchase
     */
    ACTIVE,

    /**
     * Ticket is temporarily inactive (not available for sale)
     * Can be reactivated by organizer
     */
    INACTIVE,

    /**
     * Ticket is closed - no longer selling
     * Used when tickets have been sold and organizer wants to stop sales
     * Cannot be deleted because tickets were sold
     */
    CLOSED,

    /**
     * Ticket has reached maximum capacity (sold out)
     * Automatically set when quantitySold >= totalQuantity
     */
    SOLD_OUT,

    /**
     * Ticket has been soft deleted
     * Only possible if no tickets were sold
     */
    DELETED
}