package org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.generators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventSessionEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.embedded.Recurrence;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.RecurrenceFrequency;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.SessionStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventSessionRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.EventValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSessionGenerator {

    private final EventSessionRepo eventSessionRepo;

    // Maximum sessions to generate (prevent abuse)
    private static final int MAX_SESSIONS = 100;

    /**
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
            Set<LocalDate> exceptions = new HashSet<>(recurrence.getExceptions());  // No parsing needed!

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