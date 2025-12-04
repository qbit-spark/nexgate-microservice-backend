package org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.entity.TicketEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.payload.CreateTicketRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.payload.TicketResponse;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.payload.TicketSummaryResponse;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.payload.UpdateTicketCapacityRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.payload.UpdateTicketStatusRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.service.TicketService;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.utils.TicketEntityToResponseMapper;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.EventValidationException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/e-events/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {

    private final TicketService ticketService;
    private final TicketEntityToResponseMapper ticketMapper;

    @PostMapping("/{eventId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> createTicket(@PathVariable UUID eventId, @Valid @RequestBody CreateTicketRequest request) throws ItemNotFoundException, AccessDeniedException, EventValidationException {

        log.info("Creating ticket for event: {}", eventId);

        TicketEntity ticket = ticketService.createTicket(eventId, request);
        TicketResponse response = ticketMapper.toResponse(ticket);

        return ResponseEntity.status(HttpStatus.CREATED).body(GlobeSuccessResponseBuilder.builder().success(true).httpStatus(HttpStatus.CREATED).message("Ticket created successfully").data(response).build());
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllTicketsByEvent(@PathVariable UUID eventId) throws ItemNotFoundException {

        log.info("Fetching all tickets for event: {}", eventId);

        List<TicketEntity> tickets = ticketService.getAllTicketsByEvent(eventId);
        List<TicketSummaryResponse> responses = ticketMapper.toSummaryList(tickets);

        return ResponseEntity.status(HttpStatus.OK).body(GlobeSuccessResponseBuilder.builder().success(true).httpStatus(HttpStatus.OK).message("Tickets retrieved successfully").data(responses).build());
    }

    @GetMapping("/{eventId}/{ticketId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getTicketById(@PathVariable UUID eventId, @PathVariable UUID ticketId) throws ItemNotFoundException {

        log.info("Fetching ticket: {} for event: {}", ticketId, eventId);

        TicketEntity ticket = ticketService.getTicketById(ticketId);
        TicketResponse response = ticketMapper.toResponse(ticket);

        return ResponseEntity.status(HttpStatus.OK).body(GlobeSuccessResponseBuilder.builder().success(true).httpStatus(HttpStatus.OK).message("Ticket retrieved successfully").data(response).build());
    }

    @PatchMapping("/{eventId}/{ticketId}/capacity")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateTicketCapacity(@PathVariable UUID eventId, @PathVariable UUID ticketId, @Valid @RequestBody UpdateTicketCapacityRequest request) throws ItemNotFoundException, AccessDeniedException, EventValidationException {

        log.info("Updating capacity for ticket: {}", ticketId);

        TicketEntity ticket = ticketService.updateTicketCapacity(ticketId, request);
        TicketResponse response = ticketMapper.toResponse(ticket);

        return ResponseEntity.status(HttpStatus.OK).body(GlobeSuccessResponseBuilder.builder().success(true).httpStatus(HttpStatus.OK).message("Ticket capacity updated successfully").data(response).build());
    }

    @PatchMapping("/{eventId}/{ticketId}/status")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateTicketStatus(@PathVariable UUID eventId, @PathVariable UUID ticketId, @Valid @RequestBody UpdateTicketStatusRequest request) throws ItemNotFoundException, AccessDeniedException, EventValidationException {

        log.info("Updating status for ticket: {} to {}", ticketId, request.getStatus());

        TicketEntity ticket = ticketService.updateTicketStatus(ticketId, request);
        TicketResponse response = ticketMapper.toResponse(ticket);

        return ResponseEntity.status(HttpStatus.OK).body(GlobeSuccessResponseBuilder.builder().success(true).httpStatus(HttpStatus.OK).message("Ticket status updated successfully").data(response).build());
    }

    @DeleteMapping("/{eventId}/{ticketId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteTicket(@PathVariable UUID eventId, @PathVariable UUID ticketId) throws ItemNotFoundException, AccessDeniedException, EventValidationException {

        log.info("Deleting ticket: {} for event: {}", ticketId, eventId);

        ticketService.deleteTicket(ticketId);

        return ResponseEntity.status(HttpStatus.OK).body(GlobeSuccessResponseBuilder.builder().success(true).httpStatus(HttpStatus.OK).message("Ticket deleted successfully").data(null).build());
    }
}