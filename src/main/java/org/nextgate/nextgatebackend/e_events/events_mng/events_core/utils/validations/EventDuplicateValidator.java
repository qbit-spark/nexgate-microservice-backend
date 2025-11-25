package org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.validations;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.embedded.Venue;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventFormat;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventVisibility;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads.CreateEventRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads.VenueRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventsRepo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventDuplicateValidator {

    private final EventsRepo eventsRepo;

    // Statuses to check for duplicates (only active/visible events)
    private static final List<EventStatus> CHECKABLE_STATUSES = List.of(
            EventStatus.PUBLISHED,
            EventStatus.HAPPENING
            // NOT checking: DRAFT, CANCELLED, FINISHED
    );




    public DuplicateValidationResult validateNoDuplicateForEntity(EventEntity event) {
        log.debug("Checking for duplicate events for existing event: {}", event.getId());

        List<EventEntity> similarEvents = findSimilarEventsForEntity(event);

        if (similarEvents.isEmpty()) {
            log.debug("No similar events found");
            return DuplicateValidationResult.allowed();
        }

        log.debug("Found {} potentially similar events", similarEvents.size());

        for (EventEntity existing : similarEvents) {
            if (existing.getId().equals(event.getId())) {
                continue;
            }

            if (existing.getOrganizer().getId().equals(event.getOrganizer().getId())) {
                log.debug("Skipping event {} - same organizer", existing.getId());
                continue;
            }

            if (existing.getEventVisibility() == EventVisibility.UNLISTED) {
                log.debug("Skipping event {} - unlisted event", existing.getId());
                continue;
            }

            int similarityScore = calculateSimilarityScoreForEntities(event, existing);
            log.debug("Similarity score with event {}: {}%", existing.getId(), similarityScore);

            int threshold = getSimilarityThreshold(existing.getEventVisibility());
            log.debug("Threshold for {} event: {}%", existing.getEventVisibility(), threshold);

            if (similarityScore >= threshold) {
                log.warn("Blocking duplicate event. Similarity: {}%, threshold: {}%, existing event: {}",
                        similarityScore, threshold, existing.getId());
                return DuplicateValidationResult.blocked(
                        existing,
                        similarityScore,
                        "This event appears to be a duplicate of an existing event by another organizer. " +
                                "Event: '" + existing.getTitle() + "' by " + existing.getOrganizer().getUserName() + ". " +
                                "If this is a legitimate event, please make the title, date, or location more distinct."
                );
            }

            int warningThreshold = threshold - 10;
            if (similarityScore >= warningThreshold) {
                log.info("Warning for similar event. Similarity: {}%", similarityScore);
                return DuplicateValidationResult.warning(
                        existing,
                        similarityScore,
                        "A similar event exists by another organizer (" + existing.getOrganizer().getUserName() +
                                "). Please ensure your event is unique to avoid confusion."
                );
            }
        }

        log.debug("No blocking duplicates found");
        return DuplicateValidationResult.allowed();
    }

    private List<EventEntity> findSimilarEventsForEntity(EventEntity event) {
        ZonedDateTime searchStart = event.getStartDateTime().minusDays(3);
        ZonedDateTime searchEnd = event.getStartDateTime().plusDays(3);

        log.debug("Searching for similar events from {} to {}", searchStart, searchEnd);

        List<EventEntity> similarEvents = eventsRepo
                .findByTitleContainingIgnoreCaseAndStartDateTimeBetweenAndStatusInAndIsDeletedFalse(
                        extractKeywords(event.getTitle()),
                        searchStart,
                        searchEnd,
                        CHECKABLE_STATUSES
                );

        log.debug("Found {} events matching title and date criteria", similarEvents.size());

        if (event.getEventFormat() != EventFormat.ONLINE && event.getVenue() != null) {
            similarEvents = similarEvents.stream()
                    .filter(existing -> isVenueSimilar(event.getVenue(), existing.getVenue()))
                    .collect(Collectors.toList());

            log.debug("After location filtering: {} events remain", similarEvents.size());
        }

        return similarEvents;
    }

    private int calculateSimilarityScoreForEntities(EventEntity event1, EventEntity event2) {
        int titleScore = calculateTitleSimilarity(event1.getTitle(), event2.getTitle());
        int dateScore = calculateDateSimilarity(event1.getStartDateTime(), event2.getStartDateTime());
        int locationScore = calculateVenueSimilarity(
                event1.getVenue(),
                event2.getVenue(),
                event1.getEventFormat(),
                event2.getEventFormat()
        );

        int totalScore = (titleScore * 40 + dateScore * 30 + locationScore * 30) / 100;

        log.debug("Similarity breakdown - Title: {}%, Date: {}%, Location: {}%, Total: {}%",
                titleScore, dateScore, locationScore, totalScore);

        return totalScore;
    }

    private boolean isVenueSimilar(Venue venue1, Venue venue2) {
        if (venue2 == null || venue1 == null) return false;
        if (venue1.getName() == null) return false;

        String name1 = normalizeTitle(venue1.getName());
        String name2 = normalizeTitle(venue2.getName());

        if (name1.equals(name2)) return true;
        if (name1.contains(name2) || name2.contains(name1)) return true;

        if (venue1.getCoordinates() != null && venue1.getCoordinates().getLatitude() != null &&
                venue2.getCoordinates() != null && venue2.getCoordinates().getLatitude() != null) {

            double distance = calculateDistance(
                    venue1.getCoordinates().getLatitude(), venue1.getCoordinates().getLongitude(),
                    venue2.getCoordinates().getLatitude(), venue2.getCoordinates().getLongitude()
            );

            return distance < 2.0;
        }

        return false;
    }

    private int calculateVenueSimilarity(Venue venue1, Venue venue2, EventFormat format1, EventFormat format2) {
        if (format1 == EventFormat.ONLINE && format2 == EventFormat.ONLINE) {
            return 100;
        }

        if ((format1 == EventFormat.ONLINE && format2 == EventFormat.IN_PERSON) ||
                (format1 == EventFormat.IN_PERSON && format2 == EventFormat.ONLINE)) {
            return 0;
        }

        if (venue1 == null || venue2 == null) {
            return 50;
        }

        if (venue1.getName() == null || venue2.getName() == null) {
            return 50;
        }

        String name1 = normalizeTitle(venue1.getName());
        String name2 = normalizeTitle(venue2.getName());

        if (name1.equals(name2)) {
            return 100;
        }

        if (name1.contains(name2) || name2.contains(name1)) {
            return 90;
        }

        if (venue1.getCoordinates() != null && venue1.getCoordinates().getLatitude() != null &&
                venue2.getCoordinates() != null && venue2.getCoordinates().getLatitude() != null) {

            double distance = calculateDistance(
                    venue1.getCoordinates().getLatitude(), venue1.getCoordinates().getLongitude(),
                    venue2.getCoordinates().getLatitude(), venue2.getCoordinates().getLongitude()
            );

            if (distance < 0.1) return 95;
            if (distance < 0.5) return 80;
            if (distance < 2.0) return 50;
            if (distance < 10) return 20;

            return 0;
        }

        int nameSimilarity = calculateTitleSimilarity(name1, name2);
        return nameSimilarity / 2;
    }



    /**
     * Find events that might be similar
     * Only searches active events (PUBLISHED, HAPPENING)
     */
    private List<EventEntity> findSimilarEvents(
            String title,
            ZonedDateTime startDate,
            VenueRequest venue,
            EventFormat format) {

        // Search window: ±3 days
        ZonedDateTime searchStart = startDate.minusDays(3);
        ZonedDateTime searchEnd = startDate.plusDays(3);

        log.debug("Searching for similar events from {} to {}", searchStart, searchEnd);

        // Use query derivation method - only check active statuses
        List<EventEntity> similarEvents = eventsRepo
                .findByTitleContainingIgnoreCaseAndStartDateTimeBetweenAndStatusInAndIsDeletedFalse(
                        extractKeywords(title),
                        searchStart,
                        searchEnd,
                        CHECKABLE_STATUSES
                );

        log.debug("Found {} events matching title and date criteria", similarEvents.size());

        // Filter by location in code if needed (for in-person/hybrid)
        if (format != EventFormat.ONLINE && venue != null) {
            similarEvents = similarEvents.stream()
                    .filter(event -> isLocationSimilar(venue, event.getVenue()))
                    .collect(Collectors.toList());

            log.debug("After location filtering: {} events remain", similarEvents.size());
        }

        return similarEvents;
    }

    /**
     * Get similarity threshold based on event visibility
     * Public events have stricter thresholds
     */
    private int getSimilarityThreshold(EventVisibility visibility) {
        return switch (visibility) {
            case PUBLIC -> 85;    // Strict - main fraud target
            case PRIVATE -> 90;   // More lenient - lower risk
            case UNLISTED -> 95;  // Very lenient - minimal risk
        };
    }



    /**
     * Extract main keywords from title (filter common words)
     */
    private String extractKeywords(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }

        // Remove special characters and split
        String[] words = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .split("\\s+");

        // Common words to filter out
        Set<String> commonWords = Set.of(
                "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
                "of", "with", "by", "from", "is", "are", "was", "were", "be", "been"
        );

        // Get first 3 significant words
        String keyword = Arrays.stream(words)
                .filter(word -> !commonWords.contains(word) && word.length() > 2)
                .limit(3)
                .collect(Collectors.joining(" "));

        return keyword.isEmpty() ? title : keyword;
    }

    /**
     * Check if two venues are similar (in-memory comparison)
     */
    private boolean isLocationSimilar(VenueRequest venue1, Venue venue2) {
        if (venue2 == null) return false;
        if (venue1.getName() == null) return false;

        String name1 = normalizeTitle(venue1.getName());
        String name2 = normalizeTitle(venue2.getName());

        // Check exact name match
        if (name1.equals(name2)) return true;

        // Check if one contains the other
        if (name1.contains(name2) || name2.contains(name1)) return true;

        // Check coordinates if both provided
        if (venue1.getCoordinates().getLatitude() != null && venue1.getCoordinates().getLongitude() != null &&
                venue2.getCoordinates().getLatitude() != null && venue2.getCoordinates().getLongitude() != null) {

            double distance = calculateDistance(
                    venue1.getCoordinates().getLatitude(), venue1.getCoordinates().getLongitude(),
                    venue2.getCoordinates().getLatitude(), venue2.getCoordinates().getLongitude()
            );

            // Consider similar if within 2km
            return distance < 2.0;
        }

        return false;
    }

    /**
     * Normalize title for comparison (lowercase, remove special chars)
     */
    private String normalizeTitle(String title) {
        if (title == null) return "";
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Calculate title similarity using Levenshtein distance
     * Returns similarity percentage (0-100)
     */
    private int calculateTitleSimilarity(String title1, String title2) {
        String norm1 = normalizeTitle(title1);
        String norm2 = normalizeTitle(title2);

        // Exact match
        if (norm1.equals(norm2)) {
            return 100;
        }

        // Empty strings
        if (norm1.isEmpty() || norm2.isEmpty()) {
            return 0;
        }

        // Check if one contains the other (high similarity)
        if (norm1.contains(norm2) || norm2.contains(norm1)) {
            int minLength = Math.min(norm1.length(), norm2.length());
            int maxLength = Math.max(norm1.length(), norm2.length());
            return (minLength * 90) / maxLength; // 90% similarity for containment
        }

        // Calculate Levenshtein distance
        int distance = levenshteinDistance(norm1, norm2);
        int maxLength = Math.max(norm1.length(), norm2.length());

        if (maxLength == 0) return 100;

        // Convert distance to similarity percentage
        return Math.max(0, 100 - (distance * 100 / maxLength));
    }

    /**
     * Calculate Levenshtein distance between two strings
     * Measures minimum number of edits needed to change one string into another
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        // Initialize base cases
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        // Fill the matrix
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;

                dp[i][j] = Math.min(
                        Math.min(
                                dp[i - 1][j] + 1,      // deletion
                                dp[i][j - 1] + 1       // insertion
                        ),
                        dp[i - 1][j - 1] + cost    // substitution
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * Calculate date similarity based on time difference
     * Returns similarity percentage (0-100)
     */
    private int calculateDateSimilarity(ZonedDateTime date1, ZonedDateTime date2) {
        long hoursDiff = Math.abs(
                java.time.Duration.between(date1, date2).toHours()
        );

        if (hoursDiff <= 2) return 100;      // Within 2 hours = essentially same time
        if (hoursDiff <= 6) return 90;       // Within 6 hours = very close
        if (hoursDiff <= 24) return 75;      // Same day = similar
        if (hoursDiff <= 48) return 60;      // Within 2 days = moderately similar
        if (hoursDiff <= 72) return 40;      // Within 3 days = somewhat similar

        return 0; // More than 3 days = not similar
    }

    /**
     * Calculate location similarity
     * Considers venue names, coordinates, and event format
     * Returns similarity percentage (0-100)
     */
    private int calculateLocationSimilarity(
            VenueRequest venue1,
            Venue venue2,
            EventFormat format1,
            EventFormat format2) {

        // Both online events = same location (virtual)
        if (format1 == EventFormat.ONLINE && format2 == EventFormat.ONLINE) {
            return 100;
        }

        // One online, one not (excluding hybrid) = different locations
        if ((format1 == EventFormat.ONLINE && format2 == EventFormat.IN_PERSON) ||
                (format1 == EventFormat.IN_PERSON && format2 == EventFormat.ONLINE)) {
            return 0;
        }

        // No venue data available
        if (venue1 == null || venue2 == null) {
            return 50; // Unknown - give medium score
        }

        if (venue1.getName() == null || venue2.getName() == null) {
            return 50;
        }

        // Compare venue names
        String name1 = normalizeTitle(venue1.getName());
        String name2 = normalizeTitle(venue2.getName());

        // Exact venue name match
        if (name1.equals(name2)) {
            return 100;
        }

        // One name contains the other
        if (name1.contains(name2) || name2.contains(name1)) {
            return 90;
        }

        // Check GPS coordinates if both available
        if (venue1.getCoordinates().getLatitude() != null && venue1.getCoordinates().getLongitude() != null &&
                venue2.getCoordinates().getLatitude() != null && venue2.getCoordinates().getLongitude() != null) {

            double distance = calculateDistance(
                    venue1.getCoordinates().getLatitude(),  venue1.getCoordinates().getLongitude(),
                    venue2.getCoordinates().getLatitude(), venue2.getCoordinates().getLongitude()
            );

            if (distance < 0.1) return 95;   // < 100m - practically same place
            if (distance < 0.5) return 80;   // < 500m - very close
            if (distance < 2.0) return 50;   // < 2km - same area
            if (distance < 10) return 20;    // < 10km - same city maybe

            return 0; // Too far apart
        }

        // Fallback to name similarity only (reduced weight)
        int nameSimilarity = calculateTitleSimilarity(name1, name2);
        return nameSimilarity / 2; // Reduce weight when coordinates not available
    }

    /**
     * Calculate distance between two GPS coordinates using Haversine formula
     * Returns distance in kilometers
     */
    private double calculateDistance(
            BigDecimal lat1, BigDecimal lon1,
            BigDecimal lat2, BigDecimal lon2) {

        final double EARTH_RADIUS_KM = 6371.0;

        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());

        // Haversine formula
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}

