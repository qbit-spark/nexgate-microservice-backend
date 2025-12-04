package org.nextgate.nextgatebackend.e_events.events_mng.feedbacks_reviews.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.feedbacks_reviews.entity.EventFeedbackEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.feedbacks_reviews.payloads.CreateEventFeedbackRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.feedbacks_reviews.payloads.EventFeedbackResponse;
import org.nextgate.nextgatebackend.e_events.events_mng.feedbacks_reviews.service.EventFeedbackService;
import org.nextgate.nextgatebackend.e_events.events_mng.feedbacks_reviews.utils.EventFeedbackMapper;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/e-events/feedbacks")
@RequiredArgsConstructor
public class EventFeedbackController {

    private final EventFeedbackService feedbackService;
    private final EventFeedbackMapper feedbackMapper;

    @PostMapping("/event/{eventId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> createFeedback(
            @PathVariable UUID eventId,
            @Valid @RequestBody CreateEventFeedbackRequest request)
            throws ItemNotFoundException, ItemReadyExistException, AccessDeniedException {

        log.info("Creating feedback for event: {}", eventId);

        EventFeedbackEntity feedback = feedbackService.createFeedback(eventId, request);
        EventFeedbackResponse response = feedbackMapper.toResponse(feedback);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.CREATED)
                        .message("Feedback submitted successfully")
                        .data(response)
                        .build());
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getFeedbacks(
            @PathVariable UUID eventId,
            @PageableDefault(size = 20) Pageable pageable) throws ItemNotFoundException {

        log.info("Fetching feedbacks for event: {}", eventId);

        Page<EventFeedbackEntity> feedbacks = feedbackService.getFeedbacksForEvent(eventId, pageable);
        Page<EventFeedbackResponse> responsePage = feedbacks.map(feedbackMapper::toResponse);

        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Feedbacks retrieved successfully")
                        .data(responsePage)
                        .build());
    }
}