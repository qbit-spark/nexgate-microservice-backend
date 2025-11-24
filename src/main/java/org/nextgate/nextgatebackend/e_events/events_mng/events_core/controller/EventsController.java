package org.nextgate.nextgatebackend.e_events.events_mng.events_core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventSubmissionAction;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads.CreateEventRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads.EventResponse;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.service.EventsService;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.mapper.EventEntityToResponseMapper;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.EventValidationException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/e-events")
@RequiredArgsConstructor
public class EventsController {

    private final EventsService eventsService;
    private final EventEntityToResponseMapper eventMapper;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createEvent(
            @Valid @RequestBody CreateEventRequest createEventRequest,
            @RequestParam(name = "action", defaultValue = "SAVE_DRAFT") EventSubmissionAction action)
            throws ItemNotFoundException, AccessDeniedException, EventValidationException {

        log.info("Creating event with action: {}", action);

        EventEntity createdEvent = eventsService.createEvent(createEventRequest, action);

        // Map entity to response DTO
        EventResponse eventResponse = eventMapper.toResponse(createdEvent);

        String message = action == EventSubmissionAction.PUBLISH
                ? "Event published successfully"
                : "Event saved as draft successfully";

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.CREATED)
                        .message(message)
                        .data(eventResponse)
                        .build());
    }
}