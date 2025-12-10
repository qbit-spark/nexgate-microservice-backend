package org.nextgate.nextgatebackend.e_events.events_mng.events_core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventSubmissionAction;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads.*;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.service.EventsService;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.mapper.EventEntityToResponseMapper;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.EventValidationException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.data.domain.Page;
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


    @PostMapping("/draft")
    public ResponseEntity<GlobeSuccessResponseBuilder> createEventDraft(
            @Valid @RequestBody CreateEventDraftRequest request)
            throws ItemNotFoundException, EventValidationException {

        EventEntity draft = eventsService.createEventDraft(request);
        EventResponse response = eventMapper.toResponse(draft);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.CREATED)
                        .message("Event draft created")
                        .data(response)
                        .build());
    }

    @GetMapping("/draft")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyDraft() throws ItemNotFoundException {
        EventEntity draft = eventsService.getMyCurrentEventDraft();

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.builder()
                .success(true)
                .httpStatus(HttpStatus.OK)
                .message(draft != null ? "Draft retrieved" : "No draft found")
                .data(draft != null ? eventMapper.toResponse(draft) : null)
                .build());
    }

    @DeleteMapping("/draft")
    public ResponseEntity<GlobeSuccessResponseBuilder> discardDraft() throws ItemNotFoundException {
        eventsService.discardEventDraft();

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.builder()
                .success(true)
                .httpStatus(HttpStatus.OK)
                .message("Draft discarded")
                .build());
    }

    @PatchMapping("/draft/basic-info")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateBasicInfo(
            @Valid @RequestBody UpdateEventBasicInfoRequest request)
            throws ItemNotFoundException, EventValidationException {

        EventEntity draft = eventsService.updateDraftBasicInfo(request);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.builder()
                .success(true)
                .httpStatus(HttpStatus.OK)
                .message("Basic info updated")
                .data(eventMapper.toResponse(draft))
                .build());
    }

    @PatchMapping("/draft/schedule")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateSchedule(
            @Valid @RequestBody ScheduleRequest request)
            throws ItemNotFoundException, EventValidationException {

        EventEntity draft = eventsService.updateDraftSchedule(request);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.builder()
                .success(true)
                .httpStatus(HttpStatus.OK)
                .message("Schedule updated")
                .data(eventMapper.toResponse(draft))
                .build());
    }

    @PatchMapping("/draft/location")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateLocation(
            @Valid @RequestBody UpdateEventLocationRequest request)
            throws ItemNotFoundException, EventValidationException {

        EventEntity draft = eventsService.updateDraftLocation(request);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.builder()
                .success(true)
                .httpStatus(HttpStatus.OK)
                .message("Location updated")
                .data(eventMapper.toResponse(draft))
                .build());
    }

    @PatchMapping("/draft/media")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateMedia(
            @Valid @RequestBody MediaRequest request) throws ItemNotFoundException {

        EventEntity draft = eventsService.updateDraftMedia(request);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.builder()
                .success(true)
                .httpStatus(HttpStatus.OK)
                .message("Media updated")
                .data(eventMapper.toResponse(draft))
                .build());
    }

    @PostMapping("/draft/products/{productId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> attachProduct(
            @PathVariable UUID productId)
            throws ItemNotFoundException, EventValidationException {

        EventEntity draft = eventsService.attachProductToDraft(productId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.builder()
                .success(true)
                .httpStatus(HttpStatus.OK)
                .message("Product attached")
                .data(eventMapper.toResponse(draft))
                .build());
    }

    @DeleteMapping("/draft/products/{productId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> removeProduct(
            @PathVariable UUID productId) throws ItemNotFoundException {

        EventEntity draft = eventsService.removeProductFromDraft(productId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.builder()
                .success(true)
                .httpStatus(HttpStatus.OK)
                .message("Product removed")
                .data(eventMapper.toResponse(draft))
                .build());
    }

    @PostMapping("/draft/shops/{shopId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> attachShop(
            @PathVariable UUID shopId)
            throws ItemNotFoundException, EventValidationException {

        EventEntity draft = eventsService.attachShopToDraft(shopId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.builder()
                .success(true)
                .httpStatus(HttpStatus.OK)
                .message("Shop attached")
                .data(eventMapper.toResponse(draft))
                .build());
    }

    @DeleteMapping("/draft/shops/{shopId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> removeShop(
            @PathVariable UUID shopId) throws ItemNotFoundException {

        EventEntity draft = eventsService.removeShopFromDraft(shopId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.builder()
                .success(true)
                .httpStatus(HttpStatus.OK)
                .message("Shop removed")
                .data(eventMapper.toResponse(draft))
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

    @GetMapping("/{eventId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getEventById(@PathVariable UUID eventId)
            throws ItemNotFoundException, AccessDeniedException {

        log.info("Fetching event: {}", eventId);

        EventEntity event = eventsService.getEventById(eventId);
        EventResponse eventResponse = eventMapper.toResponse(event);

        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Event retrieved successfully")
                        .data(eventResponse)
                        .build());
    }

    @GetMapping("/my-events")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyEvents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) throws ItemNotFoundException {

        log.info("Fetching my events, page: {}, size: {}", page, size);

        Page<EventEntity> events = eventsService.getMyEvents(page, size);
        Page<EventResponse> responses = events.map(eventMapper::toResponse);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Events retrieved successfully")
                        .data(responses)
                        .build());
    }

    @GetMapping("/my-events/status/{status}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyEventsByStatus(
            @PathVariable EventStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) throws ItemNotFoundException {

        log.info("Fetching my events with status: {}", status);

        Page<EventEntity> events = eventsService.getMyEventsByStatus(status, page, size);
        Page<EventResponse> responses = events.map(eventMapper::toResponse);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Events retrieved successfully")
                        .data(responses)
                        .build());
    }

}