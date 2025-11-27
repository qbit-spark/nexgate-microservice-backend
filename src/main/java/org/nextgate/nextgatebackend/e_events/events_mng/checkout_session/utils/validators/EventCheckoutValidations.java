package org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.utils.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.entity.EventCheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.payload.CreateEventCheckoutRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.repo.EventCheckoutSessionRepo;
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
import java.util.*;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventCheckoutValidations {

    private final EventsRepo eventsRepo;
    private final TicketRepo ticketRepo;
    private final WalletService walletService;
    private final EventCheckoutSessionRepo eventCheckoutSessionRepo;

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

        // ✅ minQuantityPerOrder: null or 0 = no minimum
        if (ticket.getMinQuantityPerOrder() != null
                && ticket.getMinQuantityPerOrder() > 0
                && totalQuantity < ticket.getMinQuantityPerOrder()) {
            throw new BadRequestException(
                    String.format("Minimum %d tickets per order", ticket.getMinQuantityPerOrder()));
        }

        // ✅ Check available quantity (if not unlimited)
        if (!ticket.getIsUnlimited()) {
            Integer available = ticket.getQuantityAvailable();
            if (available == null || available < totalQuantity) {
                throw new BadRequestException(
                        String.format("Only %d tickets available", available != null ? available : 0));
            }
        }

        // ✅ maxQuantityPerOrder: null or 0 = unlimited per order
        if (ticket.getMaxQuantityPerOrder() != null
                && ticket.getMaxQuantityPerOrder() > 0
                && totalQuantity > ticket.getMaxQuantityPerOrder()) {
            throw new BadRequestException(
                    String.format("Maximum %d tickets per order", ticket.getMaxQuantityPerOrder()));
        }

        // ✅ maxQuantityPerUser: already handles 0 as unlimited
        validateMaxQuantityPerUser(request, ticket, customer);

        log.debug("Quantity validated: {} tickets", totalQuantity);
    }


    private void validateMaxQuantityPerUser(
            CreateEventCheckoutRequest request,
            TicketEntity ticket,
            AccountEntity customer)
            throws BadRequestException {

        Integer maxPerUser = ticket.getMaxQuantityPerUser();

        // ✅ Both null and 0 mean unlimited
        if (maxPerUser == null || maxPerUser == 0) {
            log.debug("No max quantity per user limit (unlimited purchases allowed)");
            return;  // Skip validation - unlimited allowed
        }

        // ❌ Negative values are invalid
        if (maxPerUser < 0) {
            log.error("Invalid maxQuantityPerUser value: {} for ticket: {}", maxPerUser, ticket.getId());
            throw new BadRequestException("Invalid ticket configuration. Please contact support.");
        }

        // Calculate current order quantity
        int currentOrderQuantity = request.getTicketsForMe();

        if (request.getOtherAttendees() != null) {
            currentOrderQuantity += request.getOtherAttendees().stream()
                    .mapToInt(CreateEventCheckoutRequest.OtherAttendeeRequest::getQuantity)
                    .sum();
        }

        // Collect all identities to check (email, phone)
        Set<String> identitiesToCheck = new HashSet<>();

        // Add main buyer's identities
        identitiesToCheck.add(customer.getEmail().toLowerCase());
        if (customer.getPhoneNumber() != null) {
            identitiesToCheck.add(customer.getPhoneNumber());
        }

        // Add current order's other attendees identities
        if (request.getOtherAttendees() != null) {
            for (CreateEventCheckoutRequest.OtherAttendeeRequest attendee : request.getOtherAttendees()) {
                identitiesToCheck.add(attendee.getEmail().toLowerCase());
                identitiesToCheck.add(attendee.getPhone());
            }
        }

        // Get all completed sessions for this customer
        List<CheckoutSessionStatus> completedStatuses = List.of(
                CheckoutSessionStatus.PAYMENT_COMPLETED,
                CheckoutSessionStatus.COMPLETED
        );

        List<EventCheckoutSessionEntity> sessions =
                eventCheckoutSessionRepo.findByCustomerAndStatusIn(customer, completedStatuses);

        // Check each identity's purchase history
        Map<String, Integer> purchasesByIdentity = new HashMap<>();

        for (EventCheckoutSessionEntity session : sessions) {
            // Only check this ticket type
            if (!ticket.getId().equals(session.getTicketDetails().getTicketTypeId())) {
                continue;
            }

            // Check main buyer
            String buyerEmail = session.getCustomer().getEmail().toLowerCase();
            if (identitiesToCheck.contains(buyerEmail)) {
                purchasesByIdentity.merge(buyerEmail, session.getTicketDetails().getTicketsForBuyer(), Integer::sum);
            }

            // Check other attendees in past orders
            if (session.getTicketDetails().getOtherAttendees() != null) {
                for (EventCheckoutSessionEntity.OtherAttendee attendee : session.getTicketDetails().getOtherAttendees()) {
                    String attendeeEmail = attendee.getEmail().toLowerCase();
                    String attendeePhone = attendee.getPhone();

                    if (identitiesToCheck.contains(attendeeEmail)) {
                        purchasesByIdentity.merge(attendeeEmail, attendee.getQuantity(), Integer::sum);
                    }

                    if (identitiesToCheck.contains(attendeePhone)) {
                        purchasesByIdentity.merge(attendeePhone, attendee.getQuantity(), Integer::sum);
                    }
                }
            }
        }

        // Now check each identity in current order against the limit
        for (String identity : identitiesToCheck) {
            int previousForThisIdentity = purchasesByIdentity.getOrDefault(identity, 0);

            // For the main buyer, add their current tickets
            int currentForThisIdentity = 0;
            if (identity.equals(customer.getEmail().toLowerCase()) ||
                    identity.equals(customer.getPhoneNumber())) {
                currentForThisIdentity = request.getTicketsForMe();
            }

            // For other attendees, add their quantity
            if (request.getOtherAttendees() != null) {
                for (CreateEventCheckoutRequest.OtherAttendeeRequest attendee : request.getOtherAttendees()) {
                    if (identity.equals(attendee.getEmail().toLowerCase()) ||
                            identity.equals(attendee.getPhone())) {
                        currentForThisIdentity += attendee.getQuantity();
                    }
                }
            }

            int totalForThisIdentity = previousForThisIdentity + currentForThisIdentity;

            log.debug("Identity check | {} | Previously: {} | Current: {} | Total: {} | Max: {}",
                    maskIdentity(identity),
                    previousForThisIdentity,
                    currentForThisIdentity,
                    totalForThisIdentity,
                    maxPerUser);

            if (totalForThisIdentity > maxPerUser) {
                throw new BadRequestException(
                        String.format(
                                "Maximum %d tickets per user for '%s'. " +
                                        "The email/phone '%s' has already purchased %d ticket(s). " +
                                        "This order would add %d more ticket(s), exceeding the limit.",
                                maxPerUser,
                                ticket.getName(),
                                maskIdentity(identity),
                                previousForThisIdentity,
                                currentForThisIdentity
                        ));
            }
        }

        log.debug("Max quantity per user validated - all identities within limits");
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

    // Helper method to mask sensitive data in error messages
    private String maskIdentity(String identity) {
        if (identity.contains("@")) {
            // Email: j***@example.com
            int atIndex = identity.indexOf("@");
            return identity.charAt(0) + "***" + identity.substring(atIndex);
        } else {
            // Phone: +255***8888
            return identity.substring(0, 4) + "***" + identity.substring(identity.length() - 4);
        }
    }
}