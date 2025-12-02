package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.entity.ScannerEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads.ValidateTicketRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads.ValidateTicketResponse;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.service.ScannerService;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.service.TicketValidationService;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.utils.ScannerValidator;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.entity.EventBookingOrderEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.repo.EventBookingOrderRepo;
import org.nextgate.nextgatebackend.globe_crypto.TicketJWTService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketValidationServiceImpl implements TicketValidationService {

    private final ScannerService scannerService;
    private final EventBookingOrderRepo bookingOrderRepo;
    private final TicketJWTService ticketJWTService;
    private final ScannerValidator scannerValidator;

    @Override
    @Transactional
    public ValidateTicketResponse validateAndCheckIn(ValidateTicketRequest request)
            throws ItemNotFoundException {

        log.info("Validating ticket for scanner: {}", request.getScannerId());

        // 1. Validate scanner
        ScannerEntity scanner = validateScanner(request);

        // 2. Verify JWT signature
        TicketJWTService.JWTValidationResult jwtResult = verifyJWT(request.getJwtToken(), scanner);

        if (!jwtResult.isValid()) {
            log.warn("JWT validation failed: {}", jwtResult.getErrorMessage());
            return buildInvalidResponse(jwtResult.getErrorMessage(), scanner);
        }

        // 3. Extract ticket data from JWT
        UUID ticketInstanceId = jwtResult.getTicketInstanceId();
        UUID eventId = jwtResult.getEventId();

        log.debug("JWT valid. Ticket: {}, Event: {}", ticketInstanceId, eventId);

        // 4. Find booking order with this ticket
        EventBookingOrderEntity booking = findBookingWithTicket(ticketInstanceId);

        if (booking == null) {
            log.error("Ticket not found in database: {}", ticketInstanceId);
            return buildNotFoundResponse(ticketInstanceId, scanner);
        }

        // 5. Find the specific ticket instance
        EventBookingOrderEntity.BookedTicket ticket = findTicketInstance(booking, ticketInstanceId);

        if (ticket == null) {
            log.error("Ticket instance not found: {}", ticketInstanceId);
            return buildNotFoundResponse(ticketInstanceId, scanner);
        }

        // 6. Check if already checked in (DUPLICATE detection)
        if (ticket.getCheckedIn() != null && ticket.getCheckedIn()) {
            log.warn("DUPLICATE: Ticket already checked in: {}", ticketInstanceId);
            return buildDuplicateResponse(ticket, scanner);
        }

        // 7. Mark ticket as checked in
        markTicketAsCheckedIn(ticket, request, scanner);

        // 8. Save booking with updated ticket
        bookingOrderRepo.save(booking);

        // 9. Update scanner stats
        scanner.recordScan(true);

        // 10. TODO: Update attendance (placeholder for now)
        // updateAttendance(booking, ticket);

        log.info("✅ Ticket checked in successfully: {} for {}", ticketInstanceId, ticket.getAttendeeName());

        return buildSuccessResponse(ticket, jwtResult, scanner);
    }

    // ========================================
    // PRIVATE HELPER METHODS
    // ========================================

    /**
     * Validate scanner is active and device fingerprint matches
     */
    private ScannerEntity validateScanner(ValidateTicketRequest request) throws ItemNotFoundException {
        ScannerEntity scanner = scannerService.getByScannerId(request.getScannerId());

        // Validate scanner is active
        scannerValidator.validateForScanning(scanner);

        // Validate device fingerprint matches
        scannerValidator.validateDeviceFingerprint(scanner, request.getDeviceFingerprint());

        return scanner;
    }

    /**
     * Verify JWT signature with the event's public key
     */
    private TicketJWTService.JWTValidationResult verifyJWT(String jwt, ScannerEntity scanner) {
        return ticketJWTService.validateTicketJWT(jwt, scanner.getEvent().getRsaKeys());
    }

    /**
     * Find a booking order containing this ticket
     */
    private EventBookingOrderEntity findBookingWithTicket(UUID ticketInstanceId) {
        // Query all bookings and check if any contains this ticket
        // This is simplified - you might want to add a custom query for better performance
        return bookingOrderRepo.findAll().stream()
                .filter(booking -> booking.getBookedTickets().stream()
                        .anyMatch(ticket -> ticket.getTicketInstanceId().equals(ticketInstanceId)))
                .findFirst()
                .orElse(null);
    }

    /**
     * Find a specific ticket instance within booking
     */
    private EventBookingOrderEntity.BookedTicket findTicketInstance(
            EventBookingOrderEntity booking,
            UUID ticketInstanceId) {

        return booking.getBookedTickets().stream()
                .filter(ticket -> ticket.getTicketInstanceId().equals(ticketInstanceId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Mark ticket as checked in
     */
    private void markTicketAsCheckedIn(
            EventBookingOrderEntity.BookedTicket ticket,
            ValidateTicketRequest request,
            ScannerEntity scanner) {

        ticket.setCheckedIn(true);
        ticket.setCheckedInAt(ZonedDateTime.now());
        ticket.setCheckedInBy(scanner.getName());
        ticket.setCheckInLocation(request.getCheckInLocation() != null
                ? request.getCheckInLocation()
                : scanner.getName());
    }

    /**
     * TODO: Update attendance system (placeholder)
     */
    @SuppressWarnings("unused")
    private void updateAttendance(EventBookingOrderEntity booking, EventBookingOrderEntity.BookedTicket ticket) {
        // Placeholder for future attendance tracking
        // This will be implemented when you add attendance analytics
        log.debug("TODO: Update attendance for ticket: {}", ticket.getTicketInstanceId());
    }

    // ========================================
    // RESPONSE BUILDERS
    // ========================================

    /**
     * Build success response
     */
    private ValidateTicketResponse buildSuccessResponse(
            EventBookingOrderEntity.BookedTicket ticket,
            TicketJWTService.JWTValidationResult jwtResult,
            ScannerEntity scanner) {

        return ValidateTicketResponse.builder()
                .valid(true)
                .status("VALID")
                .message("✅ Entry granted. Welcome!")
                .ticketInstanceId(ticket.getTicketInstanceId())
                .ticketTypeName(ticket.getTicketTypeName())
                .ticketSeries(ticket.getTicketSeries())
                .attendeeName(ticket.getAttendeeName())
                .attendeeEmail(ticket.getAttendeeEmail())
                .eventName(jwtResult.getEventName())
                .bookingReference(getBookingReference(jwtResult.getPayload()))
                .alreadyCheckedIn(false)
                .currentCheckInTime(ZonedDateTime.now())
                .validationMode("ONLINE")
                .scannerName(scanner.getName())
                .build();
    }

    /**
     * Build duplicate response
     */
    private ValidateTicketResponse buildDuplicateResponse(
            EventBookingOrderEntity.BookedTicket ticket,
            ScannerEntity scanner) {

        return ValidateTicketResponse.builder()
                .valid(false)
                .status("DUPLICATE")
                .message("❌ Ticket already used. Entry denied.")
                .ticketInstanceId(ticket.getTicketInstanceId())
                .ticketTypeName(ticket.getTicketTypeName())
                .ticketSeries(ticket.getTicketSeries())
                .attendeeName(ticket.getAttendeeName())
                .alreadyCheckedIn(true)
                .previousCheckInTime(ticket.getCheckedInAt())
                .previousCheckInLocation(ticket.getCheckInLocation())
                .validationMode("ONLINE")
                .scannerName(scanner.getName())
                .build();
    }

    /**
     * Build invalid JWT response
     */
    private ValidateTicketResponse buildInvalidResponse(String errorMessage, ScannerEntity scanner) {
        return ValidateTicketResponse.builder()
                .valid(false)
                .status("INVALID_SIGNATURE")
                .message("❌ Invalid ticket. " + errorMessage)
                .validationMode("ONLINE")
                .scannerName(scanner.getName())
                .build();
    }

    /**
     * Build not found response
     */
    private ValidateTicketResponse buildNotFoundResponse(UUID ticketInstanceId, ScannerEntity scanner) {
        return ValidateTicketResponse.builder()
                .valid(false)
                .status("NOT_FOUND")
                .message("❌ Ticket not found in system")
                .ticketInstanceId(ticketInstanceId)
                .validationMode("ONLINE")
                .scannerName(scanner.getName())
                .build();
    }

    /**
     * Extract booking reference from JWT payload
     */
    private String getBookingReference(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        Object ref = payload.get("bookingReference");
        return ref != null ? ref.toString() : null;
    }
}