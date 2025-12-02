package org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.enums;


public enum AbsenteeCategory {
    ALL,                 // All absentees for the specified day
    FULL_NO_SHOW,        // Never checked in for any day
    SPECIFIC_DAY_ONLY    // Didn't check in for THIS day but attended other days
}