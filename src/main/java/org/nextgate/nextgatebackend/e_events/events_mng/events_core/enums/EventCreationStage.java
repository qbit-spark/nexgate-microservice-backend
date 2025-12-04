package org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums;

import lombok.Getter;

@Getter
public enum EventCreationStage {
    BASIC_INFO(true),           // Always required
    SCHEDULE(true),              // Always required
    LOCATION_DETAILS(true),      // Always required
    TICKETS(true),               // Always required
    MEDIA(false),                // Optional
    LINKS(false),                // Optional
    REVIEW(true);                // Always required

    private final boolean required;

    EventCreationStage(boolean required) {
        this.required = required;
    }

}