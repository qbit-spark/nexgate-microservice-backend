package org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.service;

import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.entity.TicketEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.payload.CreateTicketRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.payload.UpdateTicketCapacityRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.payload.UpdateTicketStatusRequest;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.EventValidationException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface TicketService {

    /**
     * Create a new ticket type for an event
     * Only works for DRAFT events
     * User must be event organizer or EVENT_MANAGER
     */
    TicketEntity createTicket(UUID eventId, CreateTicketRequest request)
            throws ItemNotFoundException, AccessDeniedException, EventValidationException;

    /**
     * Get single ticket by ID
     */
    TicketEntity getTicketById(UUID ticketId)
            throws ItemNotFoundException;

    /**
     * Get all tickets for an event (not deleted)
     */
    List<TicketEntity> getAllTicketsByEvent(UUID eventId)
            throws ItemNotFoundException;

    /**
     * Update ticket capacity only
     * Works for both DRAFT and PUBLISHED events
     * New capacity must be >= quantitySold
     */
    TicketEntity updateTicketCapacity(UUID ticketId, UpdateTicketCapacityRequest request)
            throws ItemNotFoundException, AccessDeniedException, EventValidationException;

    /**
     * Update ticket status (ACTIVE, INACTIVE, CLOSED)
     * Cannot manually set SOLD_OUT or DELETED
     */
    TicketEntity updateTicketStatus(UUID ticketId, UpdateTicketStatusRequest request)
            throws ItemNotFoundException, AccessDeniedException, EventValidationException;

    /**
     * Delete ticket (soft delete)
     * Only works if quantitySold = 0
     * Otherwise, must close the ticket
     */
    void deleteTicket(UUID ticketId)
            throws ItemNotFoundException, AccessDeniedException, EventValidationException;
}