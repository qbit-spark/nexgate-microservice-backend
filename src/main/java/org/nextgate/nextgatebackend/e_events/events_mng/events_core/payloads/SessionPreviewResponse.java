package org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for session preview (before saving to database)
 * Shows users what sessions will be generated from their recurrence pattern
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionPreviewResponse {

    // Total number of sessions that will be generated
    private int totalSessions;

    // Date range summary
    private DateRangeSummary dateRange;

    // First 50 sessions for preview (or all if <= 50)
    @Builder.Default
    private List<SessionPreviewDTO> sessions = new ArrayList<>();

    // Breakdown by month
    @Builder.Default
    private Map<String, Integer> byMonth = new HashMap<>();

    // Breakdown by day of week
    @Builder.Default
    private Map<String, Integer> byDayOfWeek = new HashMap<>();

    // Warnings if pattern seems problematic
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * Nested DTO for date range summary
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRangeSummary {
        private LocalDate firstSession;
        private LocalDate lastSession;
        private int durationInDays;
    }

    /**
     * Nested DTO for individual session preview
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionPreviewDTO {
        private int sessionNumber;
        private LocalDate date;
        private String dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;
        private String formattedDate;  // e.g. "Monday, Jan 6, 2025"
        private String formattedTime;  // e.g. "9:00 AM - 10:00 AM"
    }
}