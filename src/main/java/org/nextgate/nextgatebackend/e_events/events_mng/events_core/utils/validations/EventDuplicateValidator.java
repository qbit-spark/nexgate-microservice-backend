package org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.validations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.embedded.Venue;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventFormat;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventVisibility;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventsRepo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
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

    private static final List<EventStatus> CHECKABLE_STATUSES = List.of(
            EventStatus.PUBLISHED,
            EventStatus.HAPPENING
    );

    private static final Set<String> COMMON_WORDS = Set.of(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "is", "are", "was", "were", "be", "been"
    );

    private static final double EARTH_RADIUS_KM = 6371.0;

    // ========================================================================
    // PUBLIC API
    // ========================================================================

    public DuplicateValidationResult validate(EventEntity event) {
        log.debug("Checking duplicates for event: {}", event.getId());

        List<EventEntity> similarEvents = findSimilarEvents(event);

        if (similarEvents.isEmpty()) {
            return DuplicateValidationResult.allowed();
        }

        for (EventEntity existing : similarEvents) {
            // Skip self and same organizer
            if (existing.getId().equals(event.getId()) ||
                    existing.getOrganizer().getId().equals(event.getOrganizer().getId()) ||
                    existing.getEventVisibility() == EventVisibility.UNLISTED) {
                continue;
            }

            int score = calculateSimilarity(event, existing);
            int threshold = getThreshold(existing.getEventVisibility());

            log.debug("Similarity with event {}: {}% (threshold: {}%)",
                    existing.getId(), score, threshold);

            if (score >= threshold) {
                return DuplicateValidationResult.blocked(existing, score, buildBlockMessage(existing));
            }

            if (score >= threshold - 10) {
                return DuplicateValidationResult.warning(existing, score, buildWarningMessage(existing));
            }
        }

        return DuplicateValidationResult.allowed();
    }

    // ========================================================================
    // SIMILARITY CALCULATION
    // ========================================================================

    private int calculateSimilarity(EventEntity e1, EventEntity e2) {
        int titleScore = calculateTitleSimilarity(e1.getTitle(), e2.getTitle());
        int dateScore = calculateDateSimilarity(e1.getStartDateTime(), e2.getStartDateTime());
        int locationScore = calculateLocationSimilarity(e1, e2);

        // Weighted average: Title 40%, Date 30%, Location 30%
        return (titleScore * 40 + dateScore * 30 + locationScore * 30) / 100;
    }

    private int calculateTitleSimilarity(String t1, String t2) {
        String n1 = normalize(t1);
        String n2 = normalize(t2);

        if (n1.equals(n2)) return 100;
        if (n1.isEmpty() || n2.isEmpty()) return 0;
        if (n1.contains(n2) || n2.contains(n1)) {
            return (Math.min(n1.length(), n2.length()) * 90) / Math.max(n1.length(), n2.length());
        }

        int distance = levenshteinDistance(n1, n2);
        int maxLen = Math.max(n1.length(), n2.length());
        return Math.max(0, 100 - (distance * 100 / maxLen));
    }

    private int calculateDateSimilarity(ZonedDateTime d1, ZonedDateTime d2) {
        long hours = Math.abs(Duration.between(d1, d2).toHours());

        if (hours <= 2) return 100;
        if (hours <= 6) return 90;
        if (hours <= 24) return 75;
        if (hours <= 48) return 60;
        if (hours <= 72) return 40;
        return 0;
    }

    private int calculateLocationSimilarity(EventEntity e1, EventEntity e2) {
        EventFormat f1 = e1.getEventFormat();
        EventFormat f2 = e2.getEventFormat();

        // Both online
        if (f1 == EventFormat.ONLINE && f2 == EventFormat.ONLINE) return 100;

        // One online, one in-person (not hybrid)
        if ((f1 == EventFormat.ONLINE && f2 == EventFormat.IN_PERSON) ||
                (f1 == EventFormat.IN_PERSON && f2 == EventFormat.ONLINE)) return 0;

        Venue v1 = e1.getVenue();
        Venue v2 = e2.getVenue();

        if (v1 == null || v2 == null || v1.getName() == null || v2.getName() == null) {
            return 50;
        }

        String n1 = normalize(v1.getName());
        String n2 = normalize(v2.getName());

        if (n1.equals(n2)) return 100;
        if (n1.contains(n2) || n2.contains(n1)) return 90;

        // Check coordinates
        Double distance = calculateDistance(v1, v2);
        if (distance != null) {
            if (distance < 0.1) return 95;
            if (distance < 0.5) return 80;
            if (distance < 2.0) return 50;
            if (distance < 10) return 20;
            return 0;
        }

        return calculateTitleSimilarity(n1, n2) / 2;
    }

    // ========================================================================
    // SEARCH
    // ========================================================================

    private List<EventEntity> findSimilarEvents(EventEntity event) {
        ZonedDateTime start = event.getStartDateTime().minusDays(3);
        ZonedDateTime end = event.getStartDateTime().plusDays(3);
        String keywords = extractKeywords(event.getTitle());

        List<EventEntity> results = eventsRepo
                .findByTitleContainingIgnoreCaseAndStartDateTimeBetweenAndStatusInAndIsDeletedFalse(
                        keywords, start, end, CHECKABLE_STATUSES
                );

        // Filter by location for in-person/hybrid
        if (event.getEventFormat() != EventFormat.ONLINE && event.getVenue() != null) {
            results = results.stream()
                    .filter(e -> isVenueClose(event.getVenue(), e.getVenue()))
                    .collect(Collectors.toList());
        }

        return results;
    }

    private boolean isVenueClose(Venue v1, Venue v2) {
        if (v2 == null || v1.getName() == null) return false;

        String n1 = normalize(v1.getName());
        String n2 = normalize(v2.getName());

        if (n1.equals(n2) || n1.contains(n2) || n2.contains(n1)) return true;

        Double distance = calculateDistance(v1, v2);
        return distance != null && distance < 2.0;
    }

    // ========================================================================
    // HELPERS
    // ========================================================================

    private int getThreshold(EventVisibility visibility) {
        return switch (visibility) {
            case PUBLIC -> 85;
            case PRIVATE -> 90;
            case UNLISTED -> 95;
        };
    }

    private String extractKeywords(String title) {
        if (title == null || title.isBlank()) return "";

        String[] words = title.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");

        String result = Arrays.stream(words)
                .filter(w -> !COMMON_WORDS.contains(w) && w.length() > 2)
                .limit(3)
                .collect(Collectors.joining(" "));

        return result.isEmpty() ? title : result;
    }

    private String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Double calculateDistance(Venue v1, Venue v2) {
        if (v1.getCoordinates() == null || v2.getCoordinates() == null) return null;

        BigDecimal lat1 = v1.getCoordinates().getLatitude();
        BigDecimal lon1 = v1.getCoordinates().getLongitude();
        BigDecimal lat2 = v2.getCoordinates().getLatitude();
        BigDecimal lon2 = v2.getCoordinates().getLongitude();

        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return null;

        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    private String buildBlockMessage(EventEntity existing) {
        return String.format(
                "This event appears to be a duplicate of '%s' by %s. " +
                        "Please make the title, date, or location more distinct.",
                existing.getTitle(),
                existing.getOrganizer().getUserName()
        );
    }

    private String buildWarningMessage(EventEntity existing) {
        return String.format(
                "A similar event exists by %s. Please ensure your event is unique.",
                existing.getOrganizer().getUserName()
        );
    }
}