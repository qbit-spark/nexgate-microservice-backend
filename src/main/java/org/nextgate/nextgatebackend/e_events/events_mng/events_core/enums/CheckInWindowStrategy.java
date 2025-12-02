package org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums;

/**
 * Strategy for determining check-in window for events
 */
public enum CheckInWindowStrategy {

    /**
     * Check-in allowed X hours before event starts
     * Default strategy - works for most events
     * Example: Event at 18:00, earlyCheckInHours=2 → Check-in from 16:00
     */
    HOURS_BEFORE,

    /**
     * Check-in allowed during a specific time window each day
     * Good for conferences, festivals
     * Example: checkInOpensAt="08:00", checkInClosesAt="23:00"
     */
    SPECIFIC_TIME,

    /**
     * Check-in allowed anytime on event date (00:00 - 23:59)
     * Good for casual, drop-in events
     */
    ALL_DAY,

    /**
     * Check-in only during exact event time (strict)
     * No early or late check-in allowed
     */
    EXACT_TIME,

    AS_DAY_START
}