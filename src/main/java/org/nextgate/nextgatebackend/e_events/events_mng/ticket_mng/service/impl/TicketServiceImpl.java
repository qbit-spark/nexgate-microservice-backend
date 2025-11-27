package org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventCreationStage;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventFormat;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventsRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.entity.TicketEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.AttendanceMode;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.TicketStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.payload.CreateTicketRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.payload.UpdateTicketCapacityRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.payload.UpdateTicketStatusRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.repo.TicketRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.service.TicketService;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.utils.validations.TicketValidations;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.EventValidationException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepo ticketTypeRepo;
    private final EventsRepo eventsRepo;
    private final AccountRepo accountRepo;
    private final TicketValidations ticketValidations;

    @Override
    @Transactional
    public TicketEntity createTicket(UUID eventId, CreateTicketRequest request) throws ItemNotFoundException, AccessDeniedException, EventValidationException {

        log.info("Creating ticket for event: {}", eventId);

        // 1. Get an authenticated user
        AccountEntity currentUser = getAuthenticatedAccount();

        // 2. Get event
        EventEntity event = eventsRepo.findByIdAndIsDeletedFalse(eventId).orElseThrow(() -> new ItemNotFoundException("Event not found with ID: " + eventId));

        // 3. Validate user has permission (organizer)
        validateUserCanManageTickets(currentUser, event);

        // 4. Validate ticket creation
        ticketValidations.validateTicketForCreate(request, event);

        // 5. Build ticket entity
        TicketEntity ticket = buildTicketEntity(request, event, currentUser);

        // 6. Save ticket
        TicketEntity savedTicket = ticketTypeRepo.save(ticket);

        // NEW: Check if TICKETS stage requirements are met
        EventEntity eventEntity = savedTicket.getEvent();
        if (!eventEntity.isStageCompleted(EventCreationStage.TICKETS)) {
            if (areTicketRequirementsMet(eventEntity)) {
                eventEntity.markStageCompleted(EventCreationStage.TICKETS);

                if (eventEntity.getCurrentStage() == EventCreationStage.TICKETS) {
                    eventEntity.setCurrentStage(EventCreationStage.REVIEW);
                }

                eventsRepo.save(eventEntity);
                log.info("TICKETS stage marked as completed for event: {}", eventId);
            }
        }

        log.info("Ticket created successfully with ID: {} for event: {}", savedTicket.getId(), eventId);
        return savedTicket;
    }

    @Override
    @Transactional(readOnly = true)
    public TicketEntity getTicketById(UUID ticketId) throws ItemNotFoundException {
        log.debug("Fetching ticket with ID: {}", ticketId);

        return ticketTypeRepo.findByIdAndIsDeletedFalse(ticketId).orElseThrow(() -> new ItemNotFoundException("Ticket not found with ID: " + ticketId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketEntity> getAllTicketsByEvent(UUID eventId) throws ItemNotFoundException {
        log.debug("Fetching all tickets for event: {}", eventId);

        // Verify event exists
        EventEntity event = eventsRepo.findByIdAndIsDeletedFalse(eventId).orElseThrow(() -> new ItemNotFoundException("Event not found with ID: " + eventId));

        List<TicketEntity> tickets = ticketTypeRepo.findByEventAndIsDeletedFalseOrderByCreatedAtAsc(event);

        log.debug("Found {} tickets for event: {}", tickets.size(), eventId);
        return tickets;
    }

    @Override
    @Transactional
    public TicketEntity updateTicketCapacity(UUID ticketId, UpdateTicketCapacityRequest request) throws ItemNotFoundException, AccessDeniedException, EventValidationException {

        log.info("Updating capacity for ticket: {}", ticketId);

        // 1. Get an authenticated user
        AccountEntity currentUser = getAuthenticatedAccount();

        // 2. Get ticket
        TicketEntity ticket = ticketTypeRepo.findByIdAndIsDeletedFalse(ticketId).orElseThrow(() -> new ItemNotFoundException("Ticket not found with ID: " + ticketId));

        // 3. Validate user has permission
        validateUserCanManageTickets(currentUser, ticket.getEvent());

        // 4. Validate capacity update
        ticketValidations.validateCapacityUpdate(ticket, request.getNewTotalQuantity());

        // 5. Update capacity
        Integer oldCapacity = ticket.getTotalQuantity();
        ticket.setTotalQuantity(request.getNewTotalQuantity());
        ticket.setUpdatedBy(currentUser);

        // 6. Check if the status should change from SOLD_OUT to ACTIVE
        if (ticket.getStatus() == TicketStatus.SOLD_OUT && !ticket.isSoldOut()) {
            log.info("Ticket no longer sold out, changing status to ACTIVE");
            ticket.setStatus(TicketStatus.ACTIVE);
        }

        // 7. Save ticket
        TicketEntity updatedTicket = ticketTypeRepo.save(ticket);

        log.info("Ticket capacity updated from {} to {} for ticket: {}", oldCapacity, request.getNewTotalQuantity(), ticketId);
        return updatedTicket;
    }

    @Override
    @Transactional
    public TicketEntity updateTicketStatus(UUID ticketId, UpdateTicketStatusRequest request) throws ItemNotFoundException, AccessDeniedException, EventValidationException {

        log.info("Updating status for ticket: {}", ticketId);

        // 1. Get an authenticated user
        AccountEntity currentUser = getAuthenticatedAccount();

        // 2. Get ticket
        TicketEntity ticket = ticketTypeRepo.findByIdAndIsDeletedFalse(ticketId).orElseThrow(() -> new ItemNotFoundException("Ticket not found with ID: " + ticketId));

        // 3. Validate user has permission
        validateUserCanManageTickets(currentUser, ticket.getEvent());

        // 4. Validate status transition
        ticketValidations.validateStatusTransition(ticket, request.getStatus());

        // 5. Update status
        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(request.getStatus());
        ticket.setUpdatedBy(currentUser);

        // 6. Save ticket
        TicketEntity updatedTicket = ticketTypeRepo.save(ticket);

        log.info("Ticket status updated from {} to {} for ticket: {}", oldStatus, request.getStatus(), ticketId);
        return updatedTicket;
    }

    @Override
    @Transactional
    public void deleteTicket(UUID ticketId) throws ItemNotFoundException, AccessDeniedException, EventValidationException {

        log.info("Deleting ticket: {}", ticketId);

        // 1. Get an authenticated user
        AccountEntity currentUser = getAuthenticatedAccount();

        // 2. Get ticket
        TicketEntity ticket = ticketTypeRepo.findByIdAndIsDeletedFalse(ticketId).orElseThrow(() -> new ItemNotFoundException("Ticket not found with ID: " + ticketId));

        // 3. Validate user has permission
        validateUserCanManageTickets(currentUser, ticket.getEvent());

        // 4. Validate can delete (no tickets sold)
        ticketValidations.validateCanDelete(ticket);

        // 5. Softly delete
        ticket.setIsDeleted(true);
        ticket.setDeletedAt(ZonedDateTime.now());
        ticket.setDeletedBy(currentUser);
        ticket.setStatus(TicketStatus.DELETED);

        // 6. Save ticket
        ticketTypeRepo.save(ticket);

        log.info("Ticket deleted successfully: {}", ticketId);
    }

    // ========================================================================
    // PRIVATE HELPER METHODS
    // ========================================================================

    /**
     * Build TicketEntity from CreateTicketRequest
     */
    private TicketEntity buildTicketEntity(CreateTicketRequest request, EventEntity event, AccountEntity creator) {

        TicketEntity.TicketEntityBuilder builder = TicketEntity.builder().event(event).name(request.getName()).description(request.getDescription()).price(request.getPrice()).isUnlimited(request.getIsUnlimited() != null ? request.getIsUnlimited() : false).quantitySold(0).salesStartDateTime(request.getSalesStartDateTime()).salesEndDateTime(request.getSalesEndDateTime()).minQuantityPerOrder(request.getMinQuantityPerOrder() != null ? request.getMinQuantityPerOrder() : 1).maxQuantityPerOrder(request.getMaxQuantityPerOrder()).maxQuantityPerUser(request.getMaxQuantityPerUser()).checkInValidUntil(request.getCheckInValidUntil()).customCheckInDate(request.getCustomCheckInDate()).attendanceMode(request.getAttendanceMode()).inclusiveItems(request.getInclusiveItems() != null ? request.getInclusiveItems() : List.of()).isHidden(request.getIsHidden() != null ? request.getIsHidden() : false).status(TicketStatus.ACTIVE).isDeleted(false).createdBy(creator);

        // Handle quantity based on an unlimited flag
        if (request.getIsUnlimited() != null && request.getIsUnlimited()) {
            builder.totalQuantity(null); // Unlimited
        } else {
            builder.totalQuantity(request.getTotalQuantity());
        }

        return builder.build();
    }

    /**
     * Get authenticated account from security context
     */
    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ItemNotFoundException("User not authenticated");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userName = userDetails.getUsername();

        return accountRepo.findByUserName(userName).orElseThrow(() -> new ItemNotFoundException("User not found: " + userName));
    }

    /**
     * Validate user can manage tickets for an event
     */
    private void validateUserCanManageTickets(AccountEntity user, EventEntity event) throws AccessDeniedException {

        // Check if user is event organizer
        if (event.getOrganizer().getId().equals(user.getId())) {
            log.debug("User is event organizer");
            return;
        }

        // User is neither organizer nor has a required role
        log.warn("Access denied for user: {}. Not event organizer and no EVENT_MANAGER role", user.getUserName());
        throw new AccessDeniedException("Access denied. You must be the event organizer or have EVENT_MANAGER role to manage tickets.");
    }

    private boolean areTicketRequirementsMet(EventEntity event) {
        if (event.getEventFormat() == EventFormat.HYBRID) {
            long inPersonCount = ticketTypeRepo.countByEventAndAttendanceModeAndStatusAndIsDeletedFalse(
                    event, AttendanceMode.IN_PERSON, TicketStatus.ACTIVE);

            long onlineCount = ticketTypeRepo.countByEventAndAttendanceModeAndStatusAndIsDeletedFalse(
                    event, AttendanceMode.ONLINE, TicketStatus.ACTIVE);

            return inPersonCount >= 1 && onlineCount >= 1;
        } else {
            long activeCount = ticketTypeRepo.countByEventAndStatusAndIsDeletedFalse(
                    event, TicketStatus.ACTIVE);

            return activeCount >= 1;
        }
    }
}