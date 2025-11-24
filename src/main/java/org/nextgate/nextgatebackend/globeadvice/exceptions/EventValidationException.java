package org.nextgate.nextgatebackend.globeadvice.exceptions;

import lombok.Getter;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventCreationStage;

@Getter
public class EventValidationException extends Exception {

    private final EventCreationStage stage;

    // Constructor with message only (no stage)
    public EventValidationException(String message) {
        super(message);
        this.stage = null;
    }

    // Constructor with message and stage
    public EventValidationException(String message, EventCreationStage stage) {
        super(message);
        this.stage = stage;
    }
}