package org.nextgate.nextgatebackend.e_events.events_mng.analytics.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.analytics.payload.*;
import org.nextgate.nextgatebackend.e_events.events_mng.analytics.service.OrganizerAnalyticsService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/e-events/analytics")
@RequiredArgsConstructor
@Slf4j
public class OrganizerAnalyticsController {

    private final OrganizerAnalyticsService analyticsService;

    @GetMapping("/collections/summary")
    public ResponseEntity<GlobeSuccessResponseBuilder> getCollectionSummary()
            throws ItemNotFoundException {

        CollectionSummaryResponse response = analyticsService.getCollectionSummary();

        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Collection summary retrieved")
                        .data(response)
                        .build());
    }

    @GetMapping("/collections/by-event")
    public ResponseEntity<GlobeSuccessResponseBuilder> getEventRevenue(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size)
            throws ItemNotFoundException {

        Pageable pageable = PageRequest.of(page, size);
        EventRevenueResponse response = analyticsService.getEventRevenue(status, startDate, endDate, pageable);

        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Event revenue retrieved")
                        .data(response)
                        .build());
    }

    @GetMapping("/performance/{eventId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getEventPerformance(@PathVariable UUID eventId)
            throws ItemNotFoundException, AccessDeniedException {

        EventPerformanceResponse response = analyticsService.getEventPerformance(eventId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Event performance retrieved")
                        .data(response)
                        .build());
    }

    @GetMapping("/trends")
    public ResponseEntity<GlobeSuccessResponseBuilder> getRevenueTrends(
            @RequestParam(defaultValue = "MONTHLY") String period,
            @RequestParam(required = false) Integer year)
            throws ItemNotFoundException {

        RevenueTrendResponse response = analyticsService.getRevenueTrends(period, year);

        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Revenue trends retrieved")
                        .data(response)
                        .build());
    }
}