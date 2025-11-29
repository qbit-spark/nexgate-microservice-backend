package org.nextgate.nextgatebackend.e_events.events_mng.feedbacks_reviews.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventsRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.feedbacks_reviews.entity.EventFeedbackEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.feedbacks_reviews.payloads.CreateEventFeedbackRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.feedbacks_reviews.repo.EventFeedbackRepository;
import org.nextgate.nextgatebackend.e_events.events_mng.feedbacks_reviews.service.EventFeedbackService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventFeedbackServiceImpl implements EventFeedbackService {

    private final EventFeedbackRepository feedbackRepo;
    private final EventsRepo eventsRepo;
    private final AccountRepo accountRepo;

    @Override
    @Transactional
    public EventFeedbackEntity createFeedback(UUID eventId, CreateEventFeedbackRequest request)
            throws ItemNotFoundException, ItemReadyExistException, AccessDeniedException {

        AccountEntity user = getAuthenticatedAccount();

        EventEntity event = eventsRepo.findByIdAndIsDeletedFalse(eventId)
                .orElseThrow(() -> new ItemNotFoundException("Event not found: " + eventId));

        // Prevent organizer from reviewing their own event
        if (event.getOrganizer().getId().equals(user.getId())) {
            throw new AccessDeniedException("Event organizers cannot submit feedback for their own events.");
        }

        // Check for existing feedback
        if (feedbackRepo.existsByEventAndUser(event, user)) {
            throw new ItemReadyExistException("You have already provided feedback for this event");
        }

        // Create feedback
        EventFeedbackEntity feedback = EventFeedbackEntity.builder()
                .event(event)
                .user(user)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        return feedbackRepo.save(feedback);
    }

    @Override
    public Page<EventFeedbackEntity> getFeedbacksForEvent(UUID eventId, Pageable pageable)
            throws ItemNotFoundException {

        if (!eventsRepo.existsById(eventId)) {
            throw new ItemNotFoundException("Event not found: " + eventId);
        }

        return feedbackRepo.findByEventId(eventId, pageable);
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                String userName = ((UserDetails) principal).getUsername();
                return accountRepo.findByUserName(userName)
                        .orElseThrow(() -> new ItemNotFoundException("User not found"));
            }
        }
        throw new ItemNotFoundException("User not authenticated");
    }
}