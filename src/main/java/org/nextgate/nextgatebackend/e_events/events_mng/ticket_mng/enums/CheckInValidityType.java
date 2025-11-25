package org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums;

/**
 * Defines when ticket becomes invalid/expires
 */
public enum CheckInValidityType {
    /**
     * Ticket becomes invalid just before event starts
     * Use case: Early bird tickets, tickets that expire before event begins
     */
    BEFORE_EVENT_START,


    /**
     * Ticket remains valid until event ends (DEFAULT)
     * Use case: Standard tickets valid for entire event duration
     * For MULTI_DAY events, valid until end of last day
     */
    EVENT_END,

    /**
     * Ticket valid until custom date/time set by organizer
     * Use case: VIP lounge access extending beyond event, special access periods
     * Requires customValidUntil field to be set
     */
    CUSTOM
}