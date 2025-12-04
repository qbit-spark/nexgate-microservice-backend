package org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.service;

import org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.enums.AbsenteeCategory;
import org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.payload.AbsenteeListResponse;
import org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.payload.AttendanceStatsResponse;
import org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.payload.AttendeeDetailResponse;
import org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.payload.AttendeeListResponse;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AttendanceAnalyticsService {

    AttendanceStatsResponse getAttendanceStats(UUID eventId)
            throws ItemNotFoundException, AccessDeniedException;

    AttendeeListResponse getAttendees(
            UUID eventId,
            Integer dayNumber,
            UUID ticketTypeId,
            String search,
            Pageable pageable
    ) throws ItemNotFoundException, AccessDeniedException;

    AbsenteeListResponse getAbsentees(
            UUID eventId,
            Integer dayNumber,
            UUID ticketTypeId,
            AbsenteeCategory category,
            String search,
            Pageable pageable
    ) throws ItemNotFoundException, AccessDeniedException;

    AttendeeDetailResponse getAttendeeDetail(UUID eventId, UUID ticketInstanceId)
            throws ItemNotFoundException, AccessDeniedException;
}