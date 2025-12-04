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
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventDayEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventsRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.entity.TicketEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.repo.TicketRepo;
import org.nextgate.nextgatebackend.globe_crypto.TicketJWTService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
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
    private final TicketJWTService ticketJWTService;


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
                createTicketInstances(checkoutSession, ticketType, event, bookingReference);

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

        log.debug("Created ticket instance: {} with series: {}", ticketInstanceId, ticketSeries);

        return EventBookingOrderEntity.BookedTicket.builder()
                .ticketInstanceId(ticketInstanceId)
                .ticketTypeId(ticketType.getId())
                .ticketTypeName(ticketType.getName())
                .ticketSeries(ticketSeries)
                .price(ticketType.getPrice())
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


    /**
     * Generate secure JWT-based QR code for a ticket
     * The JWT contains ticket data and is signed with the event's RSA private key
     *
     * @param bookedTicket The ticket to generate QR code for
     * @param event The event this ticket belongs to
     * @param bookingReference The booking reference number
     * @return Signed JWT token (to be used as QR code content)
     */
    public String generateQRCode(
            EventBookingOrderEntity.BookedTicket bookedTicket,
            EventEntity event,
            String bookingReference
    ) {
        log.debug("Generating secure JWT QR code for ticket instance: {}", bookedTicket.getTicketInstanceId());

        // Build event schedules
        List<TicketJWTService.EventSchedule> schedules = new ArrayList<>();

        if (event.getDays() != null && !event.getDays().isEmpty()) {
            // Multi-day event - use EventDayEntity list
            for (EventDayEntity day : event.getDays()) {
                schedules.add(TicketJWTService.EventSchedule.builder()
                        .dayName(day.getDescription())
                        .startDateTime(ZonedDateTime.from(day.getStartTime()))
                        .endDateTime(ZonedDateTime.from(day.getEndTime()))
                        .description(day.getDescription())
                        .build());
            }
        } else {
            // Single day event - create single schedule
            schedules.add(TicketJWTService.EventSchedule.builder()
                    .dayName("Day 1")
                    .startDateTime(event.getStartDateTime())
                    .endDateTime(event.getEndDateTime())
                    .description(event.getTitle())
                    .build());
        }

        // Build JWT data
        TicketJWTService.TicketJWTData jwtData = TicketJWTService.TicketJWTData.builder()
                .ticketInstanceId(bookedTicket.getTicketInstanceId())
                .ticketTypeId(bookedTicket.getTicketTypeId())
                .ticketTypeName(bookedTicket.getTicketTypeName())
                .ticketSeries(bookedTicket.getTicketSeries())
                .eventId(event.getId())
                .eventName(event.getTitle())
                .eventStartDateTime(event.getStartDateTime())
                .attendeeName(bookedTicket.getAttendeeName())
                .attendeeEmail(bookedTicket.getAttendeeEmail())
                .attendeePhone(bookedTicket.getAttendeePhone())
                .attendanceMode(bookedTicket.getAttendanceMode())
                .bookingReference(bookingReference)
                .eventSchedules(schedules)
                .validFrom(bookedTicket.getValidFrom())
                .validUntil(bookedTicket.getValidUntil())
                .build();

        // Generate signed JWT
        String jwt = ticketJWTService.generateTicketJWT(jwtData, event.getRsaKeys());

        log.info("Generated secure JWT QR code for ticket: {} (length: {} chars)",
                bookedTicket.getTicketInstanceId(), jwt.length());

        return jwt;
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


    /**
     * Create ticket instances with JWT generation
     * Updated to generate JWT for each ticket after creation
     */
    public List<EventBookingOrderEntity.BookedTicket> createTicketInstances(
            EventCheckoutSessionEntity checkoutSession,
            TicketEntity ticketType,
            EventEntity event,
            String bookingReference) {

        List<EventBookingOrderEntity.BookedTicket> tickets = new ArrayList<>();
        AccountEntity buyer = checkoutSession.getCustomer();

        EventCheckoutSessionEntity.TicketCheckoutDetails details = checkoutSession.getTicketDetails();

        int ticketsForBuyer = details.getTicketsForBuyer() != null
                ? details.getTicketsForBuyer()
                : 0;

        // Create tickets for buyer
        for (int i = 0; i < ticketsForBuyer; i++) {
            EventBookingOrderEntity.BookedTicket ticket = createSingleTicketInstance(
                    ticketType,
                    buyer.getUserName(),
                    buyer.getEmail(),
                    buyer.getPhoneNumber(),
                    buyer.getUserName(),
                    buyer.getEmail(),
                    event
            );

            // Generate JWT after a ticket is created
            String jwt = generateQRCode(ticket, event, bookingReference);
            ticket.setJwtToken(jwt);
            ticket.setQrCode(jwt);

            tickets.add(ticket);
        }

        // Create tickets for other attendees
        if (details.getOtherAttendees() != null) {
            for (EventCheckoutSessionEntity.OtherAttendee attendee : details.getOtherAttendees()) {
                int quantity = attendee.getQuantity() != null ? attendee.getQuantity() : 0;

                for (int i = 0; i < quantity; i++) {
                    EventBookingOrderEntity.BookedTicket ticket = createSingleTicketInstance(
                            ticketType,
                            attendee.getName(),
                            attendee.getEmail(),
                            attendee.getPhone(),
                            buyer.getUserName(),
                            buyer.getEmail(),
                            event
                    );

                    // Generate JWT after ticket is created
                    String jwt = generateQRCode(ticket, event, bookingReference);
                    ticket.setJwtToken(jwt);
                    ticket.setQrCode(jwt);

                    tickets.add(ticket);
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