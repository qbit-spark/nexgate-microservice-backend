package org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.enums;

/**
 * Overall attendance status for a ticket holder
 */
public enum AttendanceStatus {
    FULLY_ATTENDED,      // Checked in for all event days
    PARTIALLY_ATTENDED,  // Checked in for some but not all days
    NOT_ATTENDED         // Never checked in for any day
}