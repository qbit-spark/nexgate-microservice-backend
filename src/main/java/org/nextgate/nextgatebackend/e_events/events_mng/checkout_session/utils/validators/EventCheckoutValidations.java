package org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.utils.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.payload.CreateEventCheckoutRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventsRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.entity.TicketEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.TicketStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.repo.TicketRepo;
import org.nextgate.nextgatebackend.financial_system.wallet.entity.WalletEntity;
import org.nextgate.nextgatebackend.financial_system.wallet.service.WalletService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventCheckoutValidations {

    private final EventsRepo eventsRepo;
    private final TicketRepo ticketRepo;
    private final WalletService walletService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+255[67]\\d{8}$");

    public void validateCheckoutRequest(CreateEventCheckoutRequest request, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException {

        log.debug("Starting checkout validation for user: {}", customer.getUserName());

        validateEvent(request.getEventId());
        TicketEntity ticket = validateTicket(request.getTicketTypeId(), request.getEventId());
        validateQuantities(request, ticket, customer);
        validateAttendeeData(request);
        validateWallet(request, ticket, customer);

        log.debug("Checkout validation completed successfully");
    }

    private void validateEvent(UUID eventId) throws ItemNotFoundException, BadRequestException {
        EventEntity event = eventsRepo.findByIdAndIsDeletedFalse(eventId)
                .orElseThrow(() -> new ItemNotFoundException("Event not found"));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BadRequestException("Event is not available for booking");
        }

        if (event.getStartDateTime().isBefore(ZonedDateTime.now())) {
            throw new BadRequestException("Cannot book tickets for past events");
        }

        log.debug("Event validated: {}", event.getTitle());
    }

    private TicketEntity validateTicket(UUID ticketTypeId, UUID eventId)
            throws ItemNotFoundException, BadRequestException {

        TicketEntity ticket = ticketRepo.findById(ticketTypeId)
                .orElseThrow(() -> new ItemNotFoundException("Ticket type not found"));

        if (ticket.getIsDeleted()) {
            throw new BadRequestException("Ticket type is no longer available");
        }

        if (!ticket.getEvent().getId().equals(eventId)) {
            throw new BadRequestException("Ticket does not belong to this event");
        }

        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new BadRequestException("Ticket is not active");
        }

        if (!ticket.isOnSale()) {
            throw new BadRequestException("Ticket is not currently on sale");
        }

        log.debug("Ticket validated: {}", ticket.getName());
        return ticket;
    }

    private void validateQuantities(CreateEventCheckoutRequest request, TicketEntity ticket, AccountEntity customer)
            throws BadRequestException {

        int totalQuantity = request.getTicketsForMe();

        if (request.getOtherAttendees() != null) {
            totalQuantity += request.getOtherAttendees().stream()
                    .mapToInt(CreateEventCheckoutRequest.OtherAttendeeRequest::getQuantity)
                    .sum();
        }

        if (totalQuantity == 0) {
            throw new BadRequestException("Must purchase at least 1 ticket");
        }

        if (ticket.getMinQuantityPerOrder() != null && totalQuantity < ticket.getMinQuantityPerOrder()) {
            throw new BadRequestException(
                    String.format("Minimum %d tickets per order", ticket.getMinQuantityPerOrder()));
        }

        if (!ticket.getIsUnlimited()) {
            Integer available = ticket.getQuantityAvailable();
            if (available == null || available < totalQuantity) {
                throw new BadRequestException(
                        String.format("Only %d tickets available", available != null ? available : 0));
            }
        }

        if (ticket.getMaxQuantityPerOrder() != null && totalQuantity > ticket.getMaxQuantityPerOrder()) {
            throw new BadRequestException(
                    String.format("Maximum %d tickets per order", ticket.getMaxQuantityPerOrder()));
        }

        validateMaxQuantityPerUser(request, ticket, customer);

        log.debug("Quantity validated: {} tickets", totalQuantity);
    }

    private void validateMaxQuantityPerUser(CreateEventCheckoutRequest request, TicketEntity ticket, AccountEntity customer)
            throws BadRequestException {

        if (ticket.getMaxQuantityPerUser() == null) {
            return;
        }

        int currentOrderQuantity = request.getTicketsForMe();
        Set<String> uniqueAttendees = new HashSet<>();

        uniqueAttendees.add(customer.getEmail().toLowerCase());

        if (request.getOtherAttendees() != null) {
            for (CreateEventCheckoutRequest.OtherAttendeeRequest attendee : request.getOtherAttendees()) {
                String email = attendee.getEmail().toLowerCase();
                String phone = attendee.getPhone();

                currentOrderQuantity += attendee.getQuantity();
                uniqueAttendees.add(email);
                uniqueAttendees.add(phone);
            }
        }

        int totalForAllAttendees = currentOrderQuantity;

        if (totalForAllAttendees > ticket.getMaxQuantityPerUser()) {
            throw new BadRequestException(
                    String.format("Maximum %d tickets per user. This order exceeds the limit.",
                            ticket.getMaxQuantityPerUser()));
        }

        log.debug("Max quantity per user validated");
    }

    private void validateAttendeeData(CreateEventCheckoutRequest request) throws BadRequestException {

        if (request.getOtherAttendees() == null || request.getOtherAttendees().isEmpty()) {
            return;
        }

        Set<String> emails = new HashSet<>();

        for (CreateEventCheckoutRequest.OtherAttendeeRequest attendee : request.getOtherAttendees()) {

            if (attendee.getQuantity() < 1) {
                throw new BadRequestException("Attendee quantity must be at least 1");
            }

            if (!EMAIL_PATTERN.matcher(attendee.getEmail()).matches()) {
                throw new BadRequestException("Invalid email format: " + attendee.getEmail());
            }

            if (!PHONE_PATTERN.matcher(attendee.getPhone()).matches()) {
                throw new BadRequestException("Invalid phone format. Must be Tanzania format (+255...)");
            }

            if (emails.contains(attendee.getEmail())) {
                throw new BadRequestException("Duplicate attendee email: " + attendee.getEmail());
            }
            emails.add(attendee.getEmail());
        }

        log.debug("Attendee data validated");
    }

    private void validateWallet(CreateEventCheckoutRequest request, TicketEntity ticket, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException {

        WalletEntity wallet = walletService.getWalletByAccountId(customer.getAccountId());

        if (!wallet.getIsActive()) {
            throw new BadRequestException("Wallet is not active");
        }

        int totalQuantity = request.getTicketsForMe();
        if (request.getOtherAttendees() != null) {
            totalQuantity += request.getOtherAttendees().stream()
                    .mapToInt(CreateEventCheckoutRequest.OtherAttendeeRequest::getQuantity)
                    .sum();
        }

        BigDecimal totalAmount = ticket.getPrice().multiply(BigDecimal.valueOf(totalQuantity));
        BigDecimal walletBalance = walletService.getMyWalletBalance();

        if (walletBalance.compareTo(totalAmount) < 0) {
            throw new BadRequestException(
                    String.format("Insufficient wallet balance. Required: %s TZS, Available: %s TZS",
                            totalAmount, walletBalance));
        }

        log.debug("Wallet validated: sufficient balance");
    }
}