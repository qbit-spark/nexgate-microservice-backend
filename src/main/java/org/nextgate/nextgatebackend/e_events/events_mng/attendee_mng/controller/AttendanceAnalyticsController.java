package org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.enums.AbsenteeCategory;
import org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.payload.*;
import org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.service.AttendanceAnalyticsService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/e-events/attendance")
@RequiredArgsConstructor
@Slf4j
public class AttendanceAnalyticsController {

    private final AttendanceAnalyticsService attendanceAnalyticsService;

    @GetMapping("/{eventId}/stats")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAttendanceStats(@PathVariable UUID eventId)
            throws ItemNotFoundException, AccessDeniedException {

        AttendanceStatsResponse stats = attendanceAnalyticsService.getAttendanceStats(eventId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Attendance stats retrieved")
                        .data(stats)
                        .build());
    }

    @GetMapping("/{eventId}/attendees")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAttendees(
            @PathVariable UUID eventId,
            @RequestParam(required = false) Integer dayNumber,
            @RequestParam(required = false) UUID ticketTypeId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size)
            throws ItemNotFoundException, AccessDeniedException {

        Pageable pageable = PageRequest.of(page, size);

        AttendeeListResponse response = attendanceAnalyticsService.getAttendees(
                eventId, dayNumber, ticketTypeId, search, pageable);

        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Attendees retrieved")
                        .data(response)
                        .build());
    }

    @GetMapping("/{eventId}/absentees")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAbsentees(
            @PathVariable UUID eventId,
            @RequestParam(required = false) Integer dayNumber,
            @RequestParam(required = false) UUID ticketTypeId,
            @RequestParam(required = false) AbsenteeCategory category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size)
            throws ItemNotFoundException, AccessDeniedException {

        Pageable pageable = PageRequest.of(page, size);

        AbsenteeListResponse response = attendanceAnalyticsService.getAbsentees(
                eventId, dayNumber, ticketTypeId,
                category != null ? category : AbsenteeCategory.ALL,
                search, pageable);

        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Absentees retrieved")
                        .data(response)
                        .build());
    }

    @GetMapping("/{eventId}/attendees/{ticketInstanceId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAttendeeDetail(
            @PathVariable UUID eventId,
            @PathVariable UUID ticketInstanceId)
            throws ItemNotFoundException, AccessDeniedException {

        AttendeeDetailResponse response = attendanceAnalyticsService.getAttendeeDetail(eventId, ticketInstanceId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Attendee detail retrieved")
                        .data(response)
                        .build());
    }
}