package org.nextgate.nextgatebackend.e_events.events_mng.analytics.service;

import org.nextgate.nextgatebackend.e_events.events_mng.analytics.payload.*;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface OrganizerAnalyticsService {

    CollectionSummaryResponse getCollectionSummary()
            throws ItemNotFoundException;

    EventRevenueResponse getEventRevenue(
            String status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) throws ItemNotFoundException;

    EventPerformanceResponse getEventPerformance(UUID eventId)
            throws ItemNotFoundException, AccessDeniedException;

    RevenueTrendResponse getRevenueTrends(String period, Integer year)
            throws ItemNotFoundException;
}