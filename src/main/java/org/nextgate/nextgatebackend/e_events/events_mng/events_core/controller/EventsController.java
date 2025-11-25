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

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/e-events")
@RequiredArgsConstructor
public class EventsController {

    private final EventsService eventsService;
    private final EventEntityToResponseMapper eventMapper;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createEvent(
            @Valid @RequestBody CreateEventRequest createEventRequest)
            throws ItemNotFoundException, AccessDeniedException, EventValidationException {

        log.info("Creating event as draft");

        EventEntity createdEvent = eventsService.createEvent(createEventRequest);
        EventResponse eventResponse = eventMapper.toResponse(createdEvent);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.CREATED)
                        .message("Event saved as draft successfully")
                        .data(eventResponse)
                        .build());
    }

    @PatchMapping("/{eventId}/publish")
    public ResponseEntity<GlobeSuccessResponseBuilder> publishEvent(@PathVariable UUID eventId)
            throws ItemNotFoundException, AccessDeniedException, EventValidationException {

        log.info("Publishing event: {}", eventId);

        EventEntity publishedEvent = eventsService.publishEvent(eventId);
        EventResponse eventResponse = eventMapper.toResponse(publishedEvent);

        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Event published successfully")
                        .data(eventResponse)
                        .build());
    }
}