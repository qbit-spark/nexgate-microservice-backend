package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.entity.ScannerEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.enums.TicketValidationStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads.ValidateTicketRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads.ValidateTicketResponse;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.service.ScannerService;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.service.TicketValidationService;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.utils.ScannerValidator;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.entity.EventBookingOrderEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.repo.EventBookingOrderRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.CheckInWindowStrategy;
import org.nextgate.nextgatebackend.globe_crypto.TicketJWTService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for validating and checking in tickets
 * Supports both single-day and multi-day events
 */
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

        // ========================================
        // STEP 1: VALIDATE SCANNER
        // ========================================

        ScannerEntity scanner = validateScanner(request);
        log.debug("Scanner validated: {}", scanner.getName());

        // ========================================
        // STEP 2: VERIFY JWT SIGNATURE
        // ========================================

        TicketJWTService.JWTValidationResult jwtResult = verifyJWT(request.getJwtToken(), scanner);

        if (!jwtResult.isValid()) {
            log.warn("JWT validation failed: {}", jwtResult.getErrorMessage());
            return buildInvalidResponse(jwtResult.getErrorMessage(), scanner);
        }

        // ========================================
        // STEP 3: EXTRACT EVENT SCHEDULES FROM JWT
        // ========================================

        Map<String, Object> payload = jwtResult.getPayload();
        List<Map<String, Object>> schedulesData = extractEventSchedules(payload);

        if (schedulesData == null || schedulesData.isEmpty()) {
            log.error("No event schedules found in JWT");
            return buildInvalidResponse("Invalid ticket: No event schedule information", scanner);
        }

        // ========================================
        // STEP 4: FIND CURRENT EVENT DAY
        // ========================================

        EventScheduleInfo currentDay = findCurrentDay(schedulesData, scanner);

        if (currentDay == null) {
            log.warn("Event not in session right now");
            return buildInvalidResponse("Event not in session right now. Check event date/time.", scanner);
        }

        log.debug("Current event day: {}", currentDay.getDayName());

        // ========================================
        // STEP 5: FIND BOOKING & TICKET IN DATABASE
        // ========================================

        UUID ticketInstanceId = jwtResult.getTicketInstanceId();
        UUID eventId = jwtResult.getEventId();

        log.debug("JWT valid. Ticket: {}, Event: {}", ticketInstanceId, eventId);

        // Find booking order with this ticket
        EventBookingOrderEntity booking = findBookingWithTicket(ticketInstanceId);

        if (booking == null) {
            log.error("Ticket not found in database: {}", ticketInstanceId);
            return buildNotFoundResponse(ticketInstanceId, scanner);
        }

        // Find the specific ticket instance
        EventBookingOrderEntity.BookedTicket ticket = findTicketInstance(booking, ticketInstanceId);

        if (ticket == null) {
            log.error("Ticket instance not found: {}", ticketInstanceId);
            return buildNotFoundResponse(ticketInstanceId, scanner);
        }

        // ========================================
        // STEP 6: CHECK IF ALREADY CHECKED IN FOR THIS DAY
        // ========================================

        if (ticket.isCheckedInForDay(currentDay.getDayName())) {
            log.warn("DUPLICATE: Ticket already checked in for {}", currentDay.getDayName());

            // Find the previous check-in for this day
            EventBookingOrderEntity.BookedTicket.CheckInRecord previousCheckIn =
                    ticket.getCheckInsForDay(currentDay.getDayName()).stream()
                            .findFirst()
                            .orElse(null);

            // Update scanner stats (failed scan)
            scanner.recordScan(false);

            return buildDuplicateResponse(ticket, scanner, currentDay.getDayName(), previousCheckIn, jwtResult);
        }

        // ========================================
        // STEP 7: MARK TICKET AS CHECKED IN FOR THIS DAY
        // ========================================

        addCheckInRecord(ticket, request, scanner, currentDay.getDayName());

        // Save booking with an updated ticket
        bookingOrderRepo.save(booking);

        // Update scanner stats (successful scan)
        scanner.recordScan(true);

        // TODO: Update attendance analytics (placeholder)
        // updateAttendance(booking, ticket, currentDay);

        log.info("✅ Ticket checked in successfully for {}: {} - {}",
                currentDay.getDayName(), ticketInstanceId, ticket.getAttendeeName());

        return buildSuccessResponse(ticket, currentDay, jwtResult, scanner);
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
     * Verify JWT signature with event's public key
     */
    private TicketJWTService.JWTValidationResult verifyJWT(String jwt, ScannerEntity scanner) {
        return ticketJWTService.validateTicketJWT(jwt, scanner.getEvent().getRsaKeys());
    }

    /**
     * Extract event schedules from JWT payload
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractEventSchedules(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }

        Object schedulesObj = payload.get("eventSchedules");
        if (schedulesObj instanceof List) {
            return (List<Map<String, Object>>) schedulesObj;
        }

        return null;
    }



    /**
     * Find which event day we're currently in
     * Uses event's check-in strategy to determine valid window
     */
    private EventScheduleInfo findCurrentDay(List<Map<String, Object>> schedules, ScannerEntity scanner) {
        if (schedules == null || schedules.isEmpty()) {
            return null;
        }

        ZonedDateTime now = ZonedDateTime.now();
        EventEntity event = scanner.getEvent();

        for (Map<String, Object> schedule : schedules) {
            try {
                String dayName = (String) schedule.get("dayName");
                String startTimeStr = (String) schedule.get("startDateTime");
                String endTimeStr = (String) schedule.get("endDateTime");

                if (dayName == null || startTimeStr == null || endTimeStr == null) {
                    continue;
                }

                ZonedDateTime eventStart = ZonedDateTime.parse(startTimeStr);
                ZonedDateTime eventEnd = ZonedDateTime.parse(endTimeStr);

                // Calculate check-in window based on event's strategy
                CheckInWindow window = calculateCheckInWindow(event, eventStart, eventEnd);

                // Check if current time is within check-in window
                if (now.isAfter(window.getStart()) && now.isBefore(window.getEnd())) {
                    log.debug("Current time within check-in window for {}: {} - {}",
                            dayName, window.getStart(), window.getEnd());
                    return new EventScheduleInfo(dayName, eventStart, eventEnd);
                }

            } catch (Exception e) {
                log.warn("Error parsing schedule entry: {}", e.getMessage());
                continue;
            }
        }

        return null;  // Not within any event day's check-in window
    }

    /**
     * Find booking order containing this ticket
     *
     * NOTE: This is a simple implementation for MVP
     * For production with large data, consider adding a custom query:
     * @Query("SELECT b FROM EventBookingOrderEntity b JOIN b.bookedTickets t WHERE t.ticketInstanceId = :ticketId")
     */
    private EventBookingOrderEntity findBookingWithTicket(UUID ticketInstanceId) {
        return bookingOrderRepo.findAll().stream()
                .filter(booking -> booking.getBookedTickets().stream()
                        .anyMatch(ticket -> ticket.getTicketInstanceId().equals(ticketInstanceId)))
                .findFirst()
                .orElse(null);
    }

    /**
     * Find specific ticket instance within booking
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
     * Add check-in record for current day
     */
    private void addCheckInRecord(
            EventBookingOrderEntity.BookedTicket ticket,
            ValidateTicketRequest request,
            ScannerEntity scanner,
            String dayName) {

        EventBookingOrderEntity.BookedTicket.CheckInRecord checkIn =
                EventBookingOrderEntity.BookedTicket.CheckInRecord.builder()
                        .checkInTime(ZonedDateTime.now())
                        .checkInLocation(request.getCheckInLocation() != null
                                ? request.getCheckInLocation()
                                : scanner.getName())
                        .checkedInBy(scanner.getName())
                        .dayName(dayName)
                        .scannerId(scanner.getScannerId())
                        .checkInMethod("QR_SCAN")
                        .build();

        if (ticket.getCheckIns() == null) {
            ticket.setCheckIns(new java.util.ArrayList<>());
        }

        ticket.getCheckIns().add(checkIn);

        log.debug("Added check-in record: {} at {} for {}", dayName, checkIn.getCheckInTime(), ticket.getAttendeeName());
    }

    /**
     * TODO: Update attendance system (placeholder)
     */
    @SuppressWarnings("unused")
    private void updateAttendance(
            EventBookingOrderEntity booking,
            EventBookingOrderEntity.BookedTicket ticket,
            EventScheduleInfo currentDay) {

        // Placeholder for future attendance tracking
        // This will be implemented when you add attendance analytics
        log.debug("TODO: Update attendance for ticket: {} on {}", ticket.getTicketInstanceId(), currentDay.getDayName());
    }

    // ========================================
    // RESPONSE BUILDERS
    // ========================================

    /**
     * Build success response
     */
    private ValidateTicketResponse buildSuccessResponse(
            EventBookingOrderEntity.BookedTicket ticket,
            EventScheduleInfo currentDay,
            TicketJWTService.JWTValidationResult jwtResult,
            ScannerEntity scanner) {

        return ValidateTicketResponse.builder()
                .valid(true)
                .status(TicketValidationStatus.VALID)
                .message(String.format("✅ Entry granted for %s. Welcome!", currentDay.getDayName()))
                .ticketInstanceId(ticket.getTicketInstanceId())
                .ticketTypeName(ticket.getTicketTypeName())
                .ticketSeries(ticket.getTicketSeries())
                .attendeeName(ticket.getAttendeeName())
                .attendeeEmail(ticket.getAttendeeEmail())
                .eventName(jwtResult.getEventName())
                .bookingReference(getBookingReference(jwtResult.getPayload()))
                .alreadyCheckedIn(false)
                .currentCheckInTime(ZonedDateTime.now())
                .dayName(currentDay.getDayName())
                .validationMode("ONLINE")
                .scannerName(scanner.getName())
                .build();
    }

    /**
     * Build duplicate response
     */
    private ValidateTicketResponse buildDuplicateResponse(
            EventBookingOrderEntity.BookedTicket ticket,
            ScannerEntity scanner,
            String dayName,
            EventBookingOrderEntity.BookedTicket.CheckInRecord previousCheckIn,
            TicketJWTService.JWTValidationResult jwtResult) {

        return ValidateTicketResponse.builder()
                .valid(false)
                .status(TicketValidationStatus.DUPLICATE)
                .message(String.format("❌ Ticket already used for %s. Entry denied.", dayName))
                .ticketInstanceId(ticket.getTicketInstanceId())
                .ticketTypeName(ticket.getTicketTypeName())
                .ticketSeries(ticket.getTicketSeries())
                .attendeeName(ticket.getAttendeeName())
                .attendeeEmail(ticket.getAttendeeEmail())
                .eventName(jwtResult.getEventName())
                .bookingReference(getBookingReference(jwtResult.getPayload()))
                .alreadyCheckedIn(true)
                .previousCheckInTime(previousCheckIn != null ? previousCheckIn.getCheckInTime() : null)
                .previousCheckInLocation(previousCheckIn != null ? previousCheckIn.getCheckInLocation() : null)
                .dayName(dayName)
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
                .status(TicketValidationStatus.INVALID_SIGNATURE)
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
                .status(TicketValidationStatus.NOT_FOUND)
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

    // ========================================
    // HELPER CLASS
    // ========================================

    /**
     * Helper class to hold current day info
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class EventScheduleInfo {
        private String dayName;
        private ZonedDateTime startTime;
        private ZonedDateTime endTime;
    }

    /**
     * Calculate check-in window based on event's strategy
     */
    private CheckInWindow calculateCheckInWindow(
            EventEntity event,
            ZonedDateTime eventStart,
            ZonedDateTime eventEnd) {

        CheckInWindowStrategy strategy = event.getCheckInStrategy() != null
                ? event.getCheckInStrategy()
                : CheckInWindowStrategy.AS_DAY_START;  // Default

        return switch (strategy) {
            case HOURS_BEFORE -> calculateHoursBeforeWindow(event, eventStart, eventEnd);
            case SPECIFIC_TIME -> calculateSpecificTimeWindow(event, eventStart);
            case ALL_DAY -> calculateAllDayWindow(eventStart);
            case EXACT_TIME -> new CheckInWindow(eventStart, eventEnd);
            case AS_DAY_START -> calculateAsDayStartWindow(eventStart, eventEnd, event.getLateCheckInMinutes());
            default ->
                // Fallback to HOURS_BEFORE
                    calculateAsDayStartWindow(eventStart, eventEnd, event.getLateCheckInMinutes());
        };
    }

    /**
     * HOURS_BEFORE: Check in X hours before event
     */
    private CheckInWindow calculateHoursBeforeWindow(
            EventEntity event,
            ZonedDateTime eventStart,
            ZonedDateTime eventEnd) {

        int earlyHours = event.getEarlyCheckInHours() != null
                ? event.getEarlyCheckInHours()
                : 2;  // Default 2 hours

        int lateMinutes = event.getLateCheckInMinutes() != null
                ? event.getLateCheckInMinutes()
                : 30;  // Default 30 minutes

        ZonedDateTime checkInStart = eventStart.minusHours(earlyHours);
        ZonedDateTime checkInEnd = eventEnd.plusMinutes(lateMinutes);

        log.debug("HOURS_BEFORE window: {} to {}", checkInStart, checkInEnd);
        return new CheckInWindow(checkInStart, checkInEnd);
    }

    /**
     * SPECIFIC_TIME: Check-in during specific hours each day
     */
    private CheckInWindow calculateSpecificTimeWindow(
            EventEntity event,
            ZonedDateTime eventStart) {

        String opensAt = event.getCheckInOpensAt();
        String closesAt = event.getCheckInClosesAt();

        if (opensAt == null || closesAt == null) {
            log.warn("SPECIFIC_TIME strategy selected but times not configured. Falling back to HOURS_BEFORE");
            return calculateHoursBeforeWindow(event, eventStart, eventStart.plusHours(3));
        }

        try {
            LocalDate eventDate = eventStart.toLocalDate();
            LocalTime openTime = LocalTime.parse(opensAt);  // "08:00"
            LocalTime closeTime = LocalTime.parse(closesAt);  // "23:00"

            ZonedDateTime checkInStart = ZonedDateTime.of(eventDate, openTime, eventStart.getZone());
            ZonedDateTime checkInEnd = ZonedDateTime.of(eventDate, closeTime, eventStart.getZone());

            log.debug("SPECIFIC_TIME window: {} to {}", checkInStart, checkInEnd);
            return new CheckInWindow(checkInStart, checkInEnd);

        } catch (Exception e) {
            log.error("Error parsing check-in times: {}", e.getMessage());
            return calculateHoursBeforeWindow(event, eventStart, eventStart.plusHours(3));
        }
    }

    /**
     * ALL_DAY: Check-in anytime on event date
     */
    private CheckInWindow calculateAllDayWindow(ZonedDateTime eventStart) {
        LocalDate eventDate = eventStart.toLocalDate();

        ZonedDateTime checkInStart = eventDate.atStartOfDay(eventStart.getZone());
        ZonedDateTime checkInEnd = eventDate.plusDays(1).atStartOfDay(eventStart.getZone());

        log.debug("ALL_DAY window: {} to {}", checkInStart, checkInEnd);
        return new CheckInWindow(checkInStart, checkInEnd);
    }


    /**
     * AS_DAY_START: Check-in allowed from start of the event day until event end + 30 mins
     */
    private CheckInWindow calculateAsDayStartWindow(
            ZonedDateTime eventStart,
            ZonedDateTime eventEnd,
            Integer lateMinutes) {

        ZonedDateTime checkInStart = eventStart.toLocalDate().atStartOfDay(eventStart.getZone());
        ZonedDateTime checkInEnd = eventEnd.plusMinutes(lateMinutes);

        log.debug("AS_DAY_START window: {} to {}", checkInStart, checkInEnd);
        return new CheckInWindow(checkInStart, checkInEnd);
    }

    /**
     * Helper class to hold a check-in window
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class CheckInWindow {
        private ZonedDateTime start;
        private ZonedDateTime end;
    }
}