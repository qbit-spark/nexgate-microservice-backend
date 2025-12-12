package org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum EventVisibility {
    @JsonProperty("PUBLIC")
    PUBLIC,
    @JsonProperty("PRIVATE")
    PRIVATE,
    @JsonProperty("UNLISTED")
    UNLISTED
}