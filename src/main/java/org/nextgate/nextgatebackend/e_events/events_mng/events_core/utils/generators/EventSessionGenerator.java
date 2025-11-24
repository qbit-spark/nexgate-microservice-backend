package org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.generators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventSessionEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.embedded.Recurrence;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.RecurrenceFrequency;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.SessionStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads.SessionPreviewResponse;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventSessionRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.EventValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSessionGenerator {

    private final EventSessionRepo eventSessionRepo;

    // Maximum sessions to generate (prevent abuse)
    private static final int MAX_SESSIONS = 30;

    // Maximum sessions to show in preview (show all since max is 20)
    private static final int MAX_PREVIEW_SESSIONS = 30;

    /**
     * ========================================
     * PREVIEW METHOD (NO DATABASE SAVE)
     * ========================================
     * Calculate and preview sessions WITHOUT saving to database
     * Perfect for showing users what will be generated before they confirm
     */
    public SessionPreviewResponse previewSessions(
            Recurrence recurrence,
            LocalTime startTime,
            LocalTime endTime) throws EventValidationException {

        log.info("Previewing sessions for recurrence pattern: {}", recurrence.getFrequency());

        // Calculate all occurrence dates (reusing existing logic)
        List<LocalDate> occurrenceDates = calculateOccurrences(recurrence);

        log.debug("Preview calculated {} occurrence dates", occurrenceDates.size());

        // Build preview response
        SessionPreviewResponse.SessionPreviewResponseBuilder responseBuilder = SessionPreviewResponse.builder()
                .totalSessions(occurrenceDates.size());

        // Add date range summary
        if (!occurrenceDates.isEmpty()) {
            LocalDate firstDate = occurrenceDates.get(0);
            LocalDate lastDate = occurrenceDates.get(occurrenceDates.size() - 1);

            responseBuilder.dateRange(SessionPreviewResponse.DateRangeSummary.builder()
                    .firstSession(firstDate)
                    .lastSession(lastDate)
                    .durationInDays((int) ChronoUnit.DAYS.between(firstDate, lastDate))
                    .build());
        }

        // Generate preview DTOs (limit to first 50)
        List<SessionPreviewResponse.SessionPreviewDTO> sessionPreviews = new ArrayList<>();
        int limit = Math.min(occurrenceDates.size(), MAX_PREVIEW_SESSIONS);

        for (int i = 0; i < limit; i++) {
            LocalDate date = occurrenceDates.get(i);

            sessionPreviews.add(SessionPreviewResponse.SessionPreviewDTO.builder()
                    .sessionNumber(i + 1)
                    .date(date)
                    .dayOfWeek(date.getDayOfWeek().toString())
                    .startTime(startTime)
                    .endTime(endTime)
                    .formattedDate(formatDate(date))
                    .formattedTime(formatTimeRange(startTime, endTime))
                    .build());
        }

        responseBuilder.sessions(sessionPreviews);

        // Add breakdown by month
        responseBuilder.byMonth(calculateSessionsByMonth(occurrenceDates));

        // Add breakdown by day of week
        responseBuilder.byDayOfWeek(calculateSessionsByDayOfWeek(occurrenceDates));

        // Generate warnings
        responseBuilder.warnings(generateWarnings(occurrenceDates.size(), recurrence));

        log.info("Preview generated successfully: {} total sessions", occurrenceDates.size());

        return responseBuilder.build();
    }

    /**
     * Overload: Preview sessions from an existing EventEntity (for drafts)
     */
    public SessionPreviewResponse previewSessions(EventEntity event) throws EventValidationException {
        if (event.getRecurrence() == null) {
            throw new EventValidationException("Event does not have a recurrence pattern");
        }

        return previewSessions(
                event.getRecurrence(),
                event.getStartDateTime().toLocalTime(),
                event.getEndDateTime().toLocalTime()
        );
    }

    /**
     * ========================================
     * ACTUAL GENERATION METHOD (SAVES TO DB)
     * ========================================
     * Generate all sessions for a recurring event
     */
    @Transactional
    public List<EventSessionEntity> generateSessions(EventEntity event) throws EventValidationException {

        log.info("Generating sessions for recurring event: {}", event.getId());

        // Validate recurrence pattern exists
        if (event.getRecurrence() == null) {
            throw new EventValidationException("Cannot generate sessions: recurrence pattern is missing");
        }

        Recurrence recurrence = event.getRecurrence();

        // Calculate all occurrence dates
        List<LocalDate> occurrenceDates = calculateOccurrences(recurrence);

        log.debug("Calculated {} occurrence dates", occurrenceDates.size());

        // Check session limit
        if (occurrenceDates.size() > MAX_SESSIONS) {
            throw new EventValidationException(
                    String.format("Recurrence pattern generates too many sessions (%d). Maximum allowed: %d",
                            occurrenceDates.size(), MAX_SESSIONS)
            );
        }

        // Create session entities
        List<EventSessionEntity> sessions = new ArrayList<>();
        int sessionNumber = 1;

        for (LocalDate date : occurrenceDates) {
            EventSessionEntity session = EventSessionEntity.builder()
                    .event(event)
                    .sessionDate(date)
                    .startTime(event.getStartDateTime().toLocalTime())
                    .endTime(event.getEndDateTime().toLocalTime())
                    .sessionNumber(sessionNumber++)
                    .capacity(null)  // Will be set from ticket configuration later
                    .bookedSpots(0)
                    .availableSpots(null)
                    .status(SessionStatus.SCHEDULED)
                    .isDeleted(false)
                    .build();

            sessions.add(session);
        }

        // Save all sessions
        List<EventSessionEntity> savedSessions = eventSessionRepo.saveAll(sessions);

        log.info("Successfully generated and saved {} sessions for event {}",
                savedSessions.size(), event.getId());

        return savedSessions;
    }

    /**
     * ========================================
     * HELPER METHODS
     * ========================================
     */

    /**
     * Calculate breakdown by month
     */
    private Map<String, Integer> calculateSessionsByMonth(List<LocalDate> dates) {
        Map<String, Integer> byMonth = new LinkedHashMap<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");

        for (LocalDate date : dates) {
            String monthKey = date.format(formatter);
            byMonth.merge(monthKey, 1, Integer::sum);
        }

        return byMonth;
    }

    /**
     * Calculate breakdown by day of week
     */
    private Map<String, Integer> calculateSessionsByDayOfWeek(List<LocalDate> dates) {
        Map<String, Integer> byDay = new LinkedHashMap<>();

        for (LocalDate date : dates) {
            String dayKey = date.getDayOfWeek().toString();
            byDay.merge(dayKey, 1, Integer::sum);
        }

        return byDay;
    }

    /**
     * Generate warnings based on pattern analysis
     */
    private List<String> generateWarnings(int totalSessions, Recurrence recurrence) {
        List<String> warnings = new ArrayList<>();

        // Warning: Approaching maximum sessions
        if (totalSessions >= 15 && totalSessions < 20) {
            warnings.add(String.format(
                    "This pattern generates %d sessions (approaching the limit of 20). Consider a shorter duration or less frequent interval.",
                    totalSessions
            ));
        }

        // Warning: At maximum limit
        if (totalSessions == 20) {
            warnings.add(
                    "This pattern generates exactly 20 sessions (the maximum allowed). " +
                            "If you need more, consider breaking it into multiple event series."
            );
        }

        // Warning: Very long duration (more than 6 months)
        if (recurrence.getRecurrenceStartDate() != null && recurrence.getRecurrenceEndDate() != null) {
            long months = ChronoUnit.MONTHS.between(
                    recurrence.getRecurrenceStartDate(),
                    recurrence.getRecurrenceEndDate()
            );

            if (months > 6) {
                warnings.add(String.format(
                        "This pattern spans %d months. Consider breaking it into smaller series for better management.",
                        months
                ));
            }
        }

        // Warning: Too few sessions (might want to use different event type)
        if (totalSessions <= 2) {
            warnings.add(
                    "Only " + totalSessions + " sessions will be created. " +
                            "Consider using a ONE_TIME or MULTI_DAY event instead of RECURRING."
            );
        }

        return warnings;
    }

    /**
     * Format date for display
     */
    private String formatDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy");
        return date.format(formatter);
    }

    /**
     * Format time range for display
     */
    private String formatTimeRange(LocalTime start, LocalTime end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
        return start.format(formatter) + " - " + end.format(formatter);
    }

    /**
     * Calculate all occurrence dates based on recurrence pattern
     */
    private List<LocalDate> calculateOccurrences(Recurrence recurrence) throws EventValidationException {

        List<LocalDate> dates = new ArrayList<>();

        LocalDate startDate = recurrence.getRecurrenceStartDate();
        LocalDate endDate = recurrence.getRecurrenceEndDate();

        if (startDate == null || endDate == null) {
            throw new EventValidationException("Recurrence start and end dates are required");
        }

        if (endDate.isBefore(startDate)) {
            throw new EventValidationException("Recurrence end date must be after start date");
        }

        // Route to appropriate frequency handler
        switch (recurrence.getFrequency()) {
            case DAILY -> dates = calculateDailyOccurrences(startDate, endDate, recurrence);
            case WEEKLY -> dates = calculateWeeklyOccurrences(startDate, endDate, recurrence);
            case MONTHLY -> dates = calculateMonthlyOccurrences(startDate, endDate, recurrence);
            case YEARLY -> dates = calculateYearlyOccurrences(startDate, endDate, recurrence);
        }

        // Remove exception dates
        if (recurrence.getExceptions() != null && !recurrence.getExceptions().isEmpty()) {
            Set<LocalDate> exceptions = new HashSet<>(recurrence.getExceptions());

            dates = dates.stream()
                    .filter(date -> !exceptions.contains(date))
                    .collect(Collectors.toList());

            log.debug("Removed {} exception dates", exceptions.size());
        }

        return dates;
    }

    /**
     * Calculate daily recurrence occurrences
     */
    private List<LocalDate> calculateDailyOccurrences(
            LocalDate startDate,
            LocalDate endDate,
            Recurrence recurrence) {

        List<LocalDate> dates = new ArrayList<>();
        int interval = recurrence.getInterval() != null ? recurrence.getInterval() : 1;

        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            dates.add(current);
            current = current.plusDays(interval);
        }

        log.debug("Generated {} daily occurrences", dates.size());
        return dates;
    }

    /**
     * Calculate weekly recurrence occurrences
     */
    private List<LocalDate> calculateWeeklyOccurrences(
            LocalDate startDate,
            LocalDate endDate,
            Recurrence recurrence) throws EventValidationException {

        List<LocalDate> dates = new ArrayList<>();
        int interval = recurrence.getInterval() != null ? recurrence.getInterval() : 1;

        // Get days of week
        Set<String> daysOfWeek = recurrence.getDaysOfWeek();
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            throw new EventValidationException("Weekly recurrence requires days of week");
        }

        // Convert to DayOfWeek enum
        Set<DayOfWeek> targetDays = daysOfWeek.stream()
                .map(day -> DayOfWeek.valueOf(day.toUpperCase()))
                .collect(Collectors.toSet());

        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            // Check if current day is in target days
            if (targetDays.contains(current.getDayOfWeek())) {
                dates.add(current);
            }

            current = current.plusDays(1);

            // Skip weeks based on interval
            if (current.getDayOfWeek() == DayOfWeek.MONDAY && interval > 1) {
                current = current.plusWeeks(interval - 1);
            }
        }

        log.debug("Generated {} weekly occurrences for days: {}", dates.size(), targetDays);
        return dates;
    }

    /**
     * Calculate monthly recurrence occurrences
     */
    private List<LocalDate> calculateMonthlyOccurrences(
            LocalDate startDate,
            LocalDate endDate,
            Recurrence recurrence) throws EventValidationException {

        List<LocalDate> dates = new ArrayList<>();
        int interval = recurrence.getInterval() != null ? recurrence.getInterval() : 1;
        Integer dayOfMonth = recurrence.getDayOfMonth();

        if (dayOfMonth == null) {
            throw new EventValidationException("Monthly recurrence requires day of month");
        }

        if (dayOfMonth < 1 || dayOfMonth > 31) {
            throw new EventValidationException("Day of month must be between 1 and 31");
        }

        LocalDate current = startDate.withDayOfMonth(1);  // Start from first day of start month

        while (!current.isAfter(endDate)) {
            try {
                // Try to set the day of month (may fail for February 30, etc.)
                LocalDate occurrence = current.withDayOfMonth(Math.min(dayOfMonth, current.lengthOfMonth()));

                // Only add if within date range
                if (!occurrence.isBefore(startDate) && !occurrence.isAfter(endDate)) {
                    dates.add(occurrence);
                }
            } catch (Exception e) {
                log.warn("Could not set day {} for month {}", dayOfMonth, current.getMonth());
            }

            current = current.plusMonths(interval);
        }

        log.debug("Generated {} monthly occurrences on day {}", dates.size(), dayOfMonth);
        return dates;
    }

    /**
     * Calculate yearly recurrence occurrences
     */
    private List<LocalDate> calculateYearlyOccurrences(
            LocalDate startDate,
            LocalDate endDate,
            Recurrence recurrence) {

        List<LocalDate> dates = new ArrayList<>();
        int interval = recurrence.getInterval() != null ? recurrence.getInterval() : 1;

        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            dates.add(current);
            current = current.plusYears(interval);
        }

        log.debug("Generated {} yearly occurrences", dates.size());
        return dates;
    }
}