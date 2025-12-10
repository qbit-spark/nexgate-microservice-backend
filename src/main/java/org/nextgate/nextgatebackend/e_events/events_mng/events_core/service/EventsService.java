package org.nextgate.nextgatebackend.e_events.events_mng.events_core.service;

import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventSubmissionAction;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads.*;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.EventValidationException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface EventsService {

    EventEntity createEventDraft(CreateEventDraftRequest request)
            throws ItemNotFoundException, EventValidationException;

    EventEntity getMyCurrentEventDraft() throws ItemNotFoundException;

    void discardEventDraft() throws ItemNotFoundException;

    EventEntity updateDraftBasicInfo(UpdateEventBasicInfoRequest request)
            throws ItemNotFoundException, EventValidationException;

    EventEntity updateDraftSchedule(ScheduleRequest schedule)
            throws ItemNotFoundException, EventValidationException;

    EventEntity updateDraftLocation(UpdateEventLocationRequest request)
            throws ItemNotFoundException, EventValidationException;

    EventEntity updateDraftMedia(MediaRequest media) throws ItemNotFoundException;

    EventEntity attachProductToDraft(UUID productId)
            throws ItemNotFoundException, EventValidationException;

    EventEntity attachShopToDraft(UUID shopId)
            throws ItemNotFoundException, EventValidationException;

    EventEntity removeProductFromDraft(UUID productId) throws ItemNotFoundException;

    EventEntity removeShopFromDraft(UUID shopId) throws ItemNotFoundException;

    EventEntity publishEvent(UUID eventId)
            throws ItemNotFoundException, AccessDeniedException, EventValidationException;

    EventEntity getEventById(UUID eventId) throws ItemNotFoundException, AccessDeniedException;


    Page<EventEntity> getMyEvents(int page, int size) throws ItemNotFoundException;

    Page<EventEntity> getMyEventsByStatus(EventStatus status, int page, int size)
            throws ItemNotFoundException;

}