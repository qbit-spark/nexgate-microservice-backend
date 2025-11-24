package org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums;

public enum SessionStatus {
    SCHEDULED,      // Future session, not started yet
    IN_PROGRESS,    // Currently happening
    COMPLETED,      // Session finished
    CANCELLED       // Session cancelled by organizer
}