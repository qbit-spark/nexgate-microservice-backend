package org.nextgate.nextgatebackend.e_events.events_mng.events_core.service;

import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventSubmissionAction;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads.CreateEventRequest;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.EventValidationException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.UUID;

public interface EventsService {
    EventEntity createEvent(CreateEventRequest createEventRequest)
            throws ItemNotFoundException, AccessDeniedException, EventValidationException;

    EventEntity publishEvent(UUID eventId)
            throws ItemNotFoundException, AccessDeniedException, EventValidationException;
}
