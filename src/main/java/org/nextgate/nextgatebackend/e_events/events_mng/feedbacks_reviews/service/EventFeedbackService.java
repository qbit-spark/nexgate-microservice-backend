package org.nextgate.nextgatebackend.e_events.events_mng.feedbacks_reviews.service;

import org.nextgate.nextgatebackend.e_events.events_mng.feedbacks_reviews.entity.EventFeedbackEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.feedbacks_reviews.payloads.CreateEventFeedbackRequest;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EventFeedbackService {
    EventFeedbackEntity createFeedback(UUID eventId, CreateEventFeedbackRequest request)
            throws ItemNotFoundException, ItemReadyExistException, AccessDeniedException;

    Page<EventFeedbackEntity> getFeedbacksForEvent(UUID eventId, Pageable pageable)
            throws ItemNotFoundException;
}