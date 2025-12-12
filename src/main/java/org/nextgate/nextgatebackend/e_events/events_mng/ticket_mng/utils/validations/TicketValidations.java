package org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.utils.validations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventFormat;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.entity.TicketEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.AttendanceMode;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.CheckInValidityType;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.TicketStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.payload.CreateTicketRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.payload.UpdateTicketRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.repo.TicketRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.EventValidationException;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketValidations {

    private final TicketRepo ticketTypeRepo;

    /**
     * Comprehensive validation for creating a new ticket
     */
    public void validateTicketForCreate(CreateTicketRequest request, EventEntity event)
            throws EventValidationException {

        log.debug("Validating ticket creation for event: {}", event.getId());

        // 1. Event must be in DRAFT status
        validateEventIsDraft(event);

        // 2. Validate quantity fields
        validateQuantityFields(request);

        // 3. Validate purchase limits
        validatePurchaseLimits(request);

        // 4. Validate sales period
        validateSalesPeriod(request, event);

        // 5. Validate ticket validity
        validateTicketValidity(request, event);

        // 6. Validate attendance mode for event format
        validateAttendanceMode(request, event);

        // 7. Check ticket name uniqueness
        validateTicketNameUnique(event, request.getName(), request.getAttendanceMode(), null);

        // 8. Validate inclusive items
        validateInclusiveItems(request);

        log.debug("Ticket validation passed for event: {}", event.getId());
    }

    // Update TicketValidations.validateTicketForUpdate
    public void validateTicketForUpdate(UpdateTicketRequest request, TicketEntity ticket)
            throws EventValidationException {

        log.debug("Validating ticket update for ticket: {}", ticket.getId());

        EventEntity event = ticket.getEvent();

        // Only validate fields that are being updated
        if (request.getTotalQuantity() != null) {
            validateQuantityFieldsForUpdate(request.getTotalQuantity());
        }

        if (request.getMinQuantityPerOrder() != null ||
                request.getMaxQuantityPerOrder() != null ||
                request.getMaxQuantityPerUser() != null) {
            validatePurchaseLimitsForUpdate(request, ticket);
        }

        if (request.getSalesStartDateTime() != null || request.getSalesEndDateTime() != null) {
            validateSalesPeriodForUpdate(request, ticket, event);
        }

        if (request.getCheckInValidUntil() != null || request.getCustomCheckInDate() != null) {
            validateTicketValidityForUpdate(request, ticket, event);
        }

        if (request.getAttendanceMode() != null) {
            validateAttendanceModeForUpdate(request.getAttendanceMode(), event);
        }

        if (request.getName() != null || request.getAttendanceMode() != null) {
            String nameToCheck = request.getName() != null ? request.getName() : ticket.getName();
            AttendanceMode modeToCheck = request.getAttendanceMode() != null ?
                    request.getAttendanceMode() : ticket.getAttendanceMode();
            validateTicketNameUniqueForUpdate(event, nameToCheck, modeToCheck, ticket.getId());
        }

        if (request.getInclusiveItems() != null) {
            validateInclusiveItemsForUpdate(request.getInclusiveItems());
        }

        log.debug("Ticket update validation passed");
    }

    private void validateQuantityFieldsForUpdate(Integer totalQuantity) throws EventValidationException {
        if (totalQuantity < 1) {
            throw new EventValidationException("Total quantity must be at least 1");
        }
        if (totalQuantity > 1_000_000) {
            throw new EventValidationException("Total quantity exceeds maximum allowed (1,000,000)");
        }
    }

    private void validatePurchaseLimitsForUpdate(UpdateTicketRequest request, TicketEntity ticket)
            throws EventValidationException {

        Integer min = request.getMinQuantityPerOrder() != null ?
                request.getMinQuantityPerOrder() : ticket.getMinQuantityPerOrder();
        Integer maxPerOrder = request.getMaxQuantityPerOrder() != null ?
                request.getMaxQuantityPerOrder() : ticket.getMaxQuantityPerOrder();
        Integer maxPerUser = request.getMaxQuantityPerUser() != null ?
                request.getMaxQuantityPerUser() : ticket.getMaxQuantityPerUser();

        if (min != null && min < 1) {
            throw new EventValidationException("Minimum quantity per order must be at least 1");
        }

        if (maxPerOrder != null && min != null && maxPerOrder < min) {
            throw new EventValidationException("Maximum quantity per order must be >= minimum");
        }

        if (maxPerUser != null && maxPerOrder != null && maxPerUser < maxPerOrder) {
            throw new EventValidationException("Maximum quantity per user must be >= maximum per order");
        }

        if (maxPerUser != null && min != null && maxPerOrder == null && maxPerUser < min) {
            throw new EventValidationException("Maximum quantity per user must be >= minimum per order");
        }

        if (maxPerOrder != null && maxPerOrder > 100) {
            throw new EventValidationException("Maximum quantity per order cannot exceed 100");
        }

        if (maxPerUser != null && maxPerUser > 1000) {
            throw new EventValidationException("Maximum quantity per user cannot exceed 1000");
        }
    }

    private void validateSalesPeriodForUpdate(UpdateTicketRequest request, TicketEntity ticket, EventEntity event)
            throws EventValidationException {

        ZonedDateTime salesStart = request.getSalesStartDateTime() != null ?
                request.getSalesStartDateTime() : ticket.getSalesStartDateTime();
        ZonedDateTime salesEnd = request.getSalesEndDateTime() != null ?
                request.getSalesEndDateTime() : ticket.getSalesEndDateTime();
        ZonedDateTime eventStart = event.getStartDateTime();

        if (salesStart != null && salesEnd != null) {
            if (salesEnd.isBefore(salesStart) || salesEnd.isEqual(salesStart)) {
                throw new EventValidationException("Sales end date must be after sales start date");
            }
        }

        if (salesStart != null && eventStart != null && salesStart.isAfter(eventStart)) {
            throw new EventValidationException("Sales start date cannot be after event start date");
        }

        if (salesEnd != null && eventStart != null && salesEnd.isAfter(eventStart)) {
            throw new EventValidationException("Sales end date cannot be after event start date");
        }
    }

    private void validateTicketValidityForUpdate(UpdateTicketRequest request, TicketEntity ticket, EventEntity event)
            throws EventValidationException {

        CheckInValidityType validityType = request.getCheckInValidUntil() != null ?
                request.getCheckInValidUntil() : ticket.getCheckInValidUntil();
        ZonedDateTime customValidUntil = request.getCustomCheckInDate() != null ?
                request.getCustomCheckInDate() : ticket.getCustomCheckInDate();

        if (validityType == CheckInValidityType.CUSTOM) {
            if (customValidUntil == null) {
                throw new EventValidationException("Custom valid until date is required when validity type is CUSTOM");
            }

            if (customValidUntil.isBefore(event.getStartDateTime())) {
                throw new EventValidationException("Custom valid until date cannot be before event start date");
            }

            if (event.getEndDateTime() != null) {
                ZonedDateTime maxValidUntil = event.getEndDateTime().plusYears(1);
                if (customValidUntil.isAfter(maxValidUntil)) {
                    throw new EventValidationException("Custom valid until date cannot be more than 1 year after event end");
                }
            }
        }
    }

    private void validateAttendanceModeForUpdate(AttendanceMode attendanceMode, EventEntity event)
            throws EventValidationException {

        if (event.getEventFormat() == EventFormat.HYBRID && attendanceMode == null) {
            throw new EventValidationException("Attendance mode (IN_PERSON or ONLINE) is required for HYBRID events");
        }
    }

    private void validateTicketNameUniqueForUpdate(EventEntity event, String ticketName,
                                                   AttendanceMode attendanceMode, UUID ticketId) throws EventValidationException {

        boolean exists = ticketTypeRepo.existsByEventAndNameAndAttendanceModeAndIdNotAndIsDeletedFalse(
                event, ticketName, attendanceMode, ticketId);

        if (exists) {
            if (attendanceMode != null) {
                throw new EventValidationException(
                        "A ticket with name '" + ticketName + "' and attendance mode '" +
                                attendanceMode + "' already exists for this event");
            } else {
                throw new EventValidationException("A ticket with name '" + ticketName + "' already exists for this event");
            }
        }
    }

    private void validateInclusiveItemsForUpdate(List<String> inclusiveItems) throws EventValidationException {
        if (inclusiveItems.size() > 50) {
            throw new EventValidationException("Cannot have more than 50 inclusive items per ticket");
        }

        for (String item : inclusiveItems) {
            if (item == null || item.isBlank()) {
                throw new EventValidationException("Inclusive items cannot be null or empty");
            }
            if (item.length() > 200) {
                throw new EventValidationException("Inclusive item text cannot exceed 200 characters");
            }
        }
    }


    /**
     * Validate event is in DRAFT status (tickets can only be created for draft events)
     */
    public void validateEventIsDraft(EventEntity event) throws EventValidationException {
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new EventValidationException(
                    "Tickets can only be created for events in DRAFT status. " +
                            "Current status: " + event.getStatus()
            );
        }
    }

    /**
     * Validate quantity fields (totalQuantity vs isUnlimited)
     */
    public void validateQuantityFields(CreateTicketRequest request) throws EventValidationException {
        Integer quantity = request.getTotalQuantity();

        if (quantity == null) {
            throw new EventValidationException("Total quantity is required");
        }

        if (quantity < 1) {
            throw new EventValidationException("Total quantity must be at least 1");
        }

        if (quantity > 1_000_000) {
            throw new EventValidationException("Total quantity exceeds maximum allowed (1,000,000)");
        }
    }

    /**
     * Validate purchase limits (min/max per order, max per user)
     */
    public void validatePurchaseLimits(CreateTicketRequest request) throws EventValidationException {
        Integer min = request.getMinQuantityPerOrder();
        Integer maxPerOrder = request.getMaxQuantityPerOrder();
        Integer maxPerUser = request.getMaxQuantityPerUser();

        // Min must be at least 1
        if (min != null && min < 1) {
            throw new EventValidationException(
                    "Minimum quantity per order must be at least 1"
            );
        }

        // Max per order must be >= min per order
        if (maxPerOrder != null && min != null && maxPerOrder < min) {
            throw new EventValidationException(
                    "Maximum quantity per order (" + maxPerOrder + ") " +
                            "must be greater than or equal to minimum (" + min + ")"
            );
        }

        // Max per user must be >= max per order (if both set)
        if (maxPerUser != null && maxPerOrder != null && maxPerUser < maxPerOrder) {
            throw new EventValidationException(
                    "Maximum quantity per user (" + maxPerUser + ") " +
                            "must be greater than or equal to maximum per order (" + maxPerOrder + ")"
            );
        }

        // If only max per user is set, it should be >= min per order
        if (maxPerUser != null && min != null && maxPerOrder == null && maxPerUser < min) {
            throw new EventValidationException(
                    "Maximum quantity per user (" + maxPerUser + ") " +
                            "must be greater than or equal to minimum per order (" + min + ")"
            );
        }

        // Reasonable upper limits
        if (maxPerOrder != null && maxPerOrder > 100) {
            throw new EventValidationException(
                    "Maximum quantity per order cannot exceed 100"
            );
        }

        if (maxPerUser != null && maxPerUser > 1000) {
            throw new EventValidationException(
                    "Maximum quantity per user cannot exceed 1000"
            );
        }
    }

    /**
     * Validate sales period (start/end dates)
     */
    public void validateSalesPeriod(CreateTicketRequest request, EventEntity event)
            throws EventValidationException {

        ZonedDateTime salesStart = request.getSalesStartDateTime();
        ZonedDateTime salesEnd = request.getSalesEndDateTime();
        ZonedDateTime eventStart = event.getStartDateTime();

        // If both dates provided, end must be after start
        if (salesStart != null && salesEnd != null) {
            if (salesEnd.isBefore(salesStart) || salesEnd.isEqual(salesStart)) {
                throw new EventValidationException(
                        "Sales end date must be after sales start date"
                );
            }
        }

        // Sales start cannot be after event start
        if (salesStart != null && eventStart != null) {
            if (salesStart.isAfter(eventStart)) {
                throw new EventValidationException(
                        "Sales start date cannot be after event start date"
                );
            }
        }

        // Sales end cannot be after event start (tickets must be sold before event)
        if (salesEnd != null && eventStart != null) {
            if (salesEnd.isAfter(eventStart)) {
                throw new EventValidationException(
                        "Sales end date cannot be after event start date. " +
                                "Tickets must be sold before the event begins."
                );
            }
        }

        // Sales start should not be in the past (warning only in log)
        if (salesStart != null && salesStart.isBefore(ZonedDateTime.now())) {
            log.warn("Sales start date is in the past: {}", salesStart);
        }
    }

    /**
     * Validate ticket validity period
     */
    public void validateTicketValidity(CreateTicketRequest request, EventEntity event)
            throws EventValidationException {

        CheckInValidityType validityType = request.getCheckInValidUntil();
        ZonedDateTime customValidUntil = request.getCustomCheckInDate();
        ZonedDateTime eventEnd = event.getEndDateTime();

        if (validityType == null) {
            throw new EventValidationException(
                    "Ticket validity type is required"
            );
        }

        // If CUSTOM type, customValidUntil is required
        if (validityType == CheckInValidityType.CUSTOM) {
            if (customValidUntil == null) {
                throw new EventValidationException(
                        "Custom valid until date is required when validity type is CUSTOM"
                );
            }

            // Custom date should not be before event start
            if (customValidUntil.isBefore(event.getStartDateTime())) {
                throw new EventValidationException(
                        "Custom valid until date cannot be before event start date"
                );
            }

            // Reasonable upper limit (e.g., not more than 1 year after event end)
            if (eventEnd != null) {
                ZonedDateTime maxValidUntil = eventEnd.plusYears(1);
                if (customValidUntil.isAfter(maxValidUntil)) {
                    throw new EventValidationException(
                            "Custom valid until date cannot be more than 1 year after event end"
                    );
                }
            }
        } else {
            // If not CUSTOM, customValidUntil should be null
            if (customValidUntil != null) {
                log.warn("Custom valid until date provided but validity type is not CUSTOM - will be ignored");
            }
        }
    }

    /**
     * Validate attendance mode based on event format
     */
    public void validateAttendanceMode(CreateTicketRequest request, EventEntity event)
            throws EventValidationException {

        EventFormat eventFormat = event.getEventFormat();
        AttendanceMode attendanceMode = request.getAttendanceMode();

        if (eventFormat == EventFormat.HYBRID) {
            // HYBRID events REQUIRE attendance mode
            if (attendanceMode == null) {
                throw new EventValidationException(
                        "Attendance mode (IN_PERSON or ONLINE) is required for HYBRID events"
                );
            }

        }
    }

    /**
     * Check if ticket name is unique for the event (considering attendance mode for HYBRID)
     */
    public void validateTicketNameUnique(
            EventEntity event,
            String ticketName,
            AttendanceMode attendanceMode,
            UUID excludeTicketId) throws EventValidationException {

        boolean exists;

        if (excludeTicketId == null) {
            // Creating new ticket
            exists = ticketTypeRepo.existsByEventAndNameAndAttendanceModeAndIsDeletedFalse(
                    event,
                    ticketName,
                    attendanceMode
            );
        } else {
            // Updating existing ticket
            exists = ticketTypeRepo.existsByEventAndNameAndAttendanceModeAndIdNotAndIsDeletedFalse(
                    event,
                    ticketName,
                    attendanceMode,
                    excludeTicketId
            );
        }

        if (exists) {
            if (attendanceMode != null) {
                throw new EventValidationException(
                        "A ticket with name '" + ticketName + "' and attendance mode '" +
                                attendanceMode + "' already exists for this event"
                );
            } else {
                throw new EventValidationException(
                        "A ticket with name '" + ticketName + "' already exists for this event"
                );
            }
        }
    }

    /**
     * Validate inclusive items
     */
    public void validateInclusiveItems(CreateTicketRequest request) throws EventValidationException {
        if (request.getInclusiveItems() == null) {
            return;
        }

        // Check list size
        if (request.getInclusiveItems().size() > 50) {
            throw new EventValidationException(
                    "Cannot have more than 50 inclusive items per ticket"
            );
        }

        // Check individual item length
        for (String item : request.getInclusiveItems()) {
            if (item == null || item.isBlank()) {
                throw new EventValidationException(
                        "Inclusive items cannot be null or empty"
                );
            }

            if (item.length() > 200) {
                throw new EventValidationException(
                        "Inclusive item text cannot exceed 200 characters: " + item
                );
            }
        }
    }

    /**
     * Validate capacity update
     * Can only update if new capacity >= quantity already sold
     */
    public void validateCapacityUpdate(TicketEntity ticket, Integer newCapacity)
            throws EventValidationException {

        log.debug("Validating capacity update for ticket: {}", ticket.getId());


        // New capacity must be positive
        if (newCapacity < 1) {
            throw new EventValidationException(
                    "New capacity must be at least 1"
            );
        }

        // Cannot reduce capacity below already sold quantity
        if (newCapacity < ticket.getQuantitySold()) {
            throw new EventValidationException(
                    "Cannot reduce capacity to " + newCapacity + " because " +
                            ticket.getQuantitySold() + " tickets have already been sold"
            );
        }

        // Reasonable upper limit
        if (newCapacity > 1_000_000) {
            throw new EventValidationException(
                    "Capacity cannot exceed 1,000,000"
            );
        }

        log.debug("Capacity update validation passed");
    }

    /**
     * Validate ticket can be deleted
     * Can only delete if no tickets have been sold
     */
    public void validateCanDelete(TicketEntity ticket) throws EventValidationException {
        if (!ticket.canBeDeleted()) {
            throw new EventValidationException(
                    "Cannot delete ticket '" + ticket.getName() + "' because " +
                            ticket.getQuantitySold() + " tickets have been sold. " +
                            "You can close the ticket instead to stop sales."
            );
        }
    }

    /**
     * Validate status transition
     */
    public void validateStatusTransition(TicketEntity ticket, TicketStatus newStatus)
            throws EventValidationException {

        TicketStatus currentStatus = ticket.getStatus();

        // Cannot transition from DELETED
        if (currentStatus == TicketStatus.DELETED) {
            throw new EventValidationException(
                    "Cannot change status of deleted ticket"
            );
        }

        // Cannot transition from CLOSED (permanent)
        if (currentStatus == TicketStatus.CLOSED && newStatus != TicketStatus.CLOSED) {
            throw new EventValidationException(
                    "Cannot change status of closed ticket. Closed status is permanent."
            );
        }

        // Cannot manually set SOLD_OUT (system manages this)
        if (newStatus == TicketStatus.SOLD_OUT) {
            throw new EventValidationException(
                    "Cannot manually set status to SOLD_OUT. " +
                            "This status is automatically set when tickets are sold out."
            );
        }

        // Cannot manually set DELETED (use delete endpoint)
        if (newStatus == TicketStatus.DELETED) {
            throw new EventValidationException(
                    "Cannot manually set status to DELETED. Use delete endpoint instead."
            );
        }

        // SOLD_OUT can only transition to ACTIVE (if capacity increased) or CLOSED
        if (currentStatus == TicketStatus.SOLD_OUT) {
            if (newStatus != TicketStatus.ACTIVE && newStatus != TicketStatus.CLOSED) {
                throw new EventValidationException(
                        "SOLD_OUT tickets can only be set to ACTIVE or CLOSED"
                );
            }
        }
    }

    /**
     * Validate event has required tickets before publishing
     * Called from EventValidations
     */
    public void validateEventHasRequiredTickets(EventEntity event) throws EventValidationException {
        log.debug("Validating event has required tickets for publishing: {}", event.getId());

        EventFormat eventFormat = event.getEventFormat();

        if (eventFormat == EventFormat.HYBRID) {
            // HYBRID events need at least 1 IN_PERSON and 1 ONLINE active ticket
            long inPersonCount = ticketTypeRepo.countByEventAndAttendanceModeAndStatusAndIsDeletedFalse(
                    event,
                    AttendanceMode.IN_PERSON,
                    TicketStatus.ACTIVE
            );

            long onlineCount = ticketTypeRepo.countByEventAndAttendanceModeAndStatusAndIsDeletedFalse(
                    event,
                    AttendanceMode.ONLINE,
                    TicketStatus.ACTIVE
            );

            if (inPersonCount < 1) {
                throw new EventValidationException(
                        "HYBRID events must have at least 1 active IN_PERSON ticket before publishing"
                );
            }

            if (onlineCount < 1) {
                throw new EventValidationException(
                        "HYBRID events must have at least 1 active ONLINE ticket before publishing"
                );
            }

            log.debug("HYBRID event has {} IN_PERSON and {} ONLINE tickets", inPersonCount, onlineCount);

        } else {
            // ONE_TIME/MULTI_DAY events need at least 1 active ticket
            long activeCount = ticketTypeRepo.countByEventAndStatusAndIsDeletedFalse(
                    event,
                    TicketStatus.ACTIVE
            );

            if (activeCount < 1) {
                throw new EventValidationException(
                        "Event must have at least 1 active ticket before publishing"
                );
            }

            log.debug("Event has {} active tickets", activeCount);
        }

        log.debug("Event ticket validation passed");
    }
}