package org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.entity.Roles;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.entity.EventCheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.repo.EventCheckoutSessionRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.entity.EventBookingOrderEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.entity.TicketSeriesCounterEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.enums.TicketInstanceStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.events.BookingCreatedEvent;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.repo.EventBookingOrderRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.repo.TicketSeriesCounterRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.service.EventBookingOrderService;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventsRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.entity.TicketEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.repo.TicketRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class EventBookingOrderServiceImpl implements EventBookingOrderService {

    private final EventBookingOrderRepo bookingOrderRepo;
    private final EventCheckoutSessionRepo checkoutSessionRepo;
    private final TicketSeriesCounterRepo seriesCounterRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final EventsRepo eventsRepo;
    private final TicketRepo ticketRepo;
    private final AccountRepo accountRepo;

    @Override
    @Transactional
    public void createBookingOrder(EventCheckoutSessionEntity checkoutSession) throws ItemNotFoundException {
        log.info("Creating booking order for checkout session: {}", checkoutSession.getEventId());

        // Validate required entities exist
        if (checkoutSession.getEventId() == null || checkoutSession.getTicketDetails().getTicketTypeId() == null) {
            throw new ItemNotFoundException(
                    "Event or ticket type is missing in checkout session");
        }

        EventEntity event = fetchEvent(checkoutSession.getEventId());

        TicketEntity ticketType = fetchTicketType(checkoutSession.getTicketDetails().getTicketTypeId());

        log.debug("Creating booking for event: {} with ticket type: {}",
                event.getTitle(), ticketType.getName());

        String bookingReference = generateBookingReference();

        List<EventBookingOrderEntity.BookedTicket> bookedTickets =
                createTicketInstances(checkoutSession, ticketType, event);

        log.info("Created {} ticket instances for booking", bookedTickets.size());

        EventBookingOrderEntity bookingOrder = buildBookingOrder(
                checkoutSession,
                event,
                bookingReference,
                bookedTickets
        );

        EventBookingOrderEntity savedBooking = saveBookingOrder(bookingOrder);

        log.info("Successfully created booking order: {} with reference: {}",
                savedBooking.getBookingId(), bookingReference);

        publishBookingCreatedEvent(
                savedBooking,           // ← Full EventBookingOrderEntity
                event,                  // ← Full EventEntity
                bookedTickets,          // ← List<EventBookingOrderEntity.BookedTicket>
                checkoutSession.getTicketDetails().getSendTicketsToAttendees()
        );

    }


    @Override
    public EventBookingOrderEntity getBookingById(UUID bookingId) throws ItemNotFoundException, AccessDeniedException {
        log.debug("Fetching booking: {}", bookingId);

        AccountEntity currentUser = getAuthenticatedAccount();

        EventBookingOrderEntity booking = bookingOrderRepo.findById(bookingId)
                .orElseThrow(() -> new ItemNotFoundException("Booking not found: " + bookingId));

        // Check if the user can view this booking
        boolean isCustomer = booking.getCustomer().getId().equals(currentUser.getId());
        boolean isOrganizer = booking.getEvent().getCreatedBy().getId().equals(currentUser.getId());
        boolean haveAmongTheRoles = validateRole(currentUser, "ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN");

        if (!isCustomer && !isOrganizer && !haveAmongTheRoles) {
            throw new AccessDeniedException("You don't have permission to view this booking");
        }

        return booking;
    }

    @Override
    public List<EventBookingOrderEntity> getMyBookings() throws ItemNotFoundException {

        AccountEntity currentUser = getAuthenticatedAccount();

        return bookingOrderRepo.findByCustomerOrderByBookedAtDesc(currentUser);
    }

    public EventCheckoutSessionEntity fetchCheckoutSession(UUID checkoutSessionId) throws ItemNotFoundException {
        log.debug("Fetching checkout session: {}", checkoutSessionId);

        EventCheckoutSessionEntity checkoutSession = checkoutSessionRepo.findById(checkoutSessionId)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Checkout session not found: " + checkoutSessionId));

        // Validate required entities exist
        if (checkoutSession.getEventId() == null || checkoutSession.getTicketDetails().getTicketTypeId() == null) {
            throw new ItemNotFoundException(
                    "Event or ticket type is missing in checkout session: " + checkoutSessionId);
        }

        return checkoutSession;
    }

    public String generateBookingReference() {
        String shortUuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String reference = "EVT-" + shortUuid;

        log.debug("Generated booking reference: {}", reference);
        return reference;
    }

    public EventBookingOrderEntity buildBookingOrder(
            EventCheckoutSessionEntity checkoutSession,
            EventEntity event,
            String bookingReference,
            List<EventBookingOrderEntity.BookedTicket> bookedTickets) {

        log.debug("Building booking order entity");

        AccountEntity customer = checkoutSession.getCustomer();

        return EventBookingOrderEntity.builder()
                .bookingReference(bookingReference)
                .event(event)
                .customer(customer)
                .checkoutSessionId(checkoutSession.getSessionId())
                // Event snapshot
                .eventTitle(event.getTitle())
                .eventStartDateTime(event.getStartDateTime().toLocalDateTime())
                .eventEndDateTime(event.getEndDateTime().toLocalDateTime())
                .eventTimezone(event.getTimezone())
                .eventLocation(buildEventLocation(event))
                .eventFormat(event.getEventFormat() != null ? event.getEventFormat().name() : null)
                .virtualDetails(event.getVirtualDetails())
                // Organizer snapshot
                .organizerName(getOrganizerName(event))
                .organizerEmail(getOrganizerEmail(event))
                .organizerPhone(getOrganizerPhone(event))
                // Tickets and pricing
                .bookedTickets(bookedTickets)
                .subtotal(checkoutSession.getTotalAmount())
                .total(checkoutSession.getTotalAmount())
                .build();
    }
    

    public EventBookingOrderEntity.BookedTicket createSingleTicketInstance(
            TicketEntity ticketType,
            String attendeeName,
            String attendeeEmail,
            String attendeePhone,
            String buyerName,
            String buyerEmail,
            EventEntity event) {

        UUID ticketInstanceId = UUID.randomUUID();
        String ticketSeries = generateTicketSeries(ticketType.getId(), ticketType.getName());
        String qrCode = generateQRCode(ticketInstanceId);

        log.debug("Created ticket instance: {} with series: {}", ticketInstanceId, ticketSeries);

        return EventBookingOrderEntity.BookedTicket.builder()
                .ticketInstanceId(ticketInstanceId)
                .ticketTypeId(ticketType.getId())
                .ticketTypeName(ticketType.getName())
                .ticketSeries(ticketSeries)
                .price(ticketType.getPrice())
                .qrCode(qrCode)
                .attendanceMode(ticketType.getAttendanceMode() != null
                        ? ticketType.getAttendanceMode().name()
                        : null)
                // Attendee info
                .attendeeName(attendeeName)
                .attendeeEmail(attendeeEmail)
                .attendeePhone(attendeePhone)
                // Buyer info
                .buyerName(buyerName)
                .buyerEmail(buyerEmail)
                // Status and validity
                .status(TicketInstanceStatus.ACTIVE)
                .checkedIn(false)
                .validFrom(event.getStartDateTime())
                .validUntil(event.getEndDateTime())
                .build();
    }

    public String generateTicketSeries(UUID ticketTypeId, String ticketTypeName) {
        log.debug("Generating ticket series for ticket type: {}", ticketTypeId);

        // Get or create a counter for this ticket type
        TicketSeriesCounterEntity counter = seriesCounterRepo.findByTicketTypeId(ticketTypeId)
                .orElseGet(() -> {
                    log.debug("Creating new series counter for ticket type: {}", ticketTypeId);
                    TicketSeriesCounterEntity newCounter = TicketSeriesCounterEntity.builder()
                            .ticketTypeId(ticketTypeId)
                            .currentCounter(0)
                            .build();
                    return seriesCounterRepo.save(newCounter);
                });

        // Get the next counter-value and save
        Integer nextCounter = counter.getNextCounter();
        seriesCounterRepo.save(counter);

        // Format: {TICKET_CODE}-{COUNTER}
        String ticketCode = extractTicketCode(ticketTypeName);
        String series = String.format("%s-%04d", ticketCode, nextCounter);

        log.debug("Generated ticket series: {}", series);
        return series;
    }

    public String generateQRCode(UUID ticketInstanceId) {
        // PLACEHOLDER: Simple UUID-based QR code
        // TODO: Implement secure QR code generation with encryption/signing
        // Options for future:
        // 1. JWT with ticketInstanceId + eventId + signature
        // 2. AES encrypted payload
        // 3. HMAC-signed token

        log.debug("Generating QR code for ticket instance: {} (using simple UUID)", ticketInstanceId);
        return ticketInstanceId.toString();
    }

    public EventBookingOrderEntity saveBookingOrder(EventBookingOrderEntity bookingOrder) {
        log.debug("Saving booking order to database");
        return bookingOrderRepo.save(bookingOrder);
    }

    private void publishBookingCreatedEvent(
            EventBookingOrderEntity bookingOrder,
            EventEntity event,
            List<EventBookingOrderEntity.BookedTicket> allTickets,
            Boolean sendTicketsToAttendees) {

        log.debug("Publishing BookingCreatedEvent for booking: {}", bookingOrder.getBookingReference());

        BookingCreatedEvent bookingCreatedEvent = BookingCreatedEvent.builder()
                .source(this)
                .bookingOrder(bookingOrder)
                .event(event)
                .allTickets(allTickets)
                .sendTicketsToAttendees(sendTicketsToAttendees)
                .build();

        eventPublisher.publishEvent(bookingCreatedEvent);

        log.info("✓ BookingCreatedEvent published | {} | {} tickets | SendToAttendees: {}",
                bookingOrder.getBookingReference(),
                allTickets.size(),
                sendTicketsToAttendees);
    }


    // ========================================
    // PRIVATE HELPER METHODS
    // ========================================
    /**
     * Extract ticket code from ticket type name
     * Takes first 5 characters or full name if shorter
     * Removes spaces and converts to uppercase
     */
    private String extractTicketCode(String ticketTypeName) {
        if (ticketTypeName == null || ticketTypeName.isBlank()) {
            return "TICK";
        }

        String code = ticketTypeName.length() >= 5
                ? ticketTypeName.substring(0, 5)
                : ticketTypeName;

        return code.toUpperCase().replaceAll("\\s+", "");
    }

    /**
     * Build a full event location string from venue name and address
     */
    private String buildEventLocation(EventEntity event) {
        if (event.getVenue().getName() == null && event.getVenue().getAddress() == null) {
            return null;
        }

        StringBuilder location = new StringBuilder();
        if (event.getVenue().getName() != null) {
            location.append(event.getVenue().getName());
        }
        if (event.getVenue().getAddress() != null) {
            if (!location.isEmpty()) {
                location.append(", ");
            }
            location.append(event.getVenue().getAddress());
        }

        return location.toString();
    }

    /**
     * Get organizer name safely with fallback
     */
    private String getOrganizerName(EventEntity event) {
        return event.getCreatedBy() != null ? event.getCreatedBy().getUserName() : "Unknown";
    }

    /**
     * Get organizer email safely
     */
    private String getOrganizerEmail(EventEntity event) {
        return event.getCreatedBy() != null ? event.getCreatedBy().getEmail() : null;
    }

    /**
     * Get organized phone safely
     */
    private String getOrganizerPhone(EventEntity event) {
        return event.getCreatedBy() != null ? event.getCreatedBy().getPhoneNumber() : null;
    }

    public EventEntity fetchEvent(UUID eventId) {
        return eventsRepo.findById(eventId)
                .orElseThrow(() -> new RuntimeException(
                        "Event not found: " + eventId));
    }

    public TicketEntity fetchTicketType(UUID ticketTypeId) {
        return ticketRepo.findById(ticketTypeId)
                .orElseThrow(() -> new RuntimeException(
                        "Ticket type not found: " + ticketTypeId));
    }



    // Update createTicketInstances to use JSONB fields:
    public List<EventBookingOrderEntity.BookedTicket> createTicketInstances(
            EventCheckoutSessionEntity checkoutSession,
            TicketEntity ticketType,
            EventEntity event) {

        List<EventBookingOrderEntity.BookedTicket> tickets = new ArrayList<>();
        AccountEntity buyer = checkoutSession.getCustomer();

        EventCheckoutSessionEntity.TicketCheckoutDetails details = checkoutSession.getTicketDetails();

        int ticketsForBuyer = details.getTicketsForBuyer() != null
                ? details.getTicketsForBuyer()
                : 0;

        for (int i = 0; i < ticketsForBuyer; i++) {
            tickets.add(createSingleTicketInstance(
                    ticketType,
                    buyer.getUserName(),
                    buyer.getEmail(),
                    buyer.getPhoneNumber(),
                    buyer.getUserName(),
                    buyer.getEmail(),
                    event
            ));
        }

        if (details.getOtherAttendees() != null) {
            for (EventCheckoutSessionEntity.OtherAttendee attendee : details.getOtherAttendees()) {
                int quantity = attendee.getQuantity() != null ? attendee.getQuantity() : 0;

                for (int i = 0; i < quantity; i++) {
                    tickets.add(createSingleTicketInstance(
                            ticketType,
                            attendee.getName(),
                            attendee.getEmail(),
                            attendee.getPhone(),
                            buyer.getUserName(),
                            buyer.getEmail(),
                            event
                    ));
                }
            }
        }

        return tickets;
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
        }
        throw new ItemNotFoundException("User not authenticated");
    }

    public boolean validateRole(AccountEntity account, String... requiredRoles) throws AccessDeniedException {
        if (account == null) {
            throw new AccessDeniedException("Account not found");
        }

        if (account.getRoles() == null || account.getRoles().isEmpty()) {
            throw new AccessDeniedException("Account has no roles assigned");
        }

        // Get account's role names
        Set<String> accountRoleNames = account.getRoles().stream()
                .map(Roles::getRoleName)
                .collect(Collectors.toSet());

        // Check if account has any of the required roles
        boolean hasRequiredRole = Arrays.stream(requiredRoles)
                .anyMatch(accountRoleNames::contains);

        if (!hasRequiredRole) {
            log.warn("Access denied for user: {}. Required roles: {}, User roles: {}",
                    account.getUserName(), Arrays.toString(requiredRoles), accountRoleNames);
            throw new AccessDeniedException("Access denied. Required roles: " + Arrays.toString(requiredRoles));
        }
        return hasRequiredRole;
    }
}