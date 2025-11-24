package org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo;

import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventSessionEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventSessionRepo extends JpaRepository<EventSessionEntity, UUID> {

    // Find all sessions for an event
    List<EventSessionEntity> findByEventIdAndIsDeletedFalseOrderBySessionDateAsc(UUID eventId);

    // Find sessions by status
    List<EventSessionEntity> findByEventIdAndStatusAndIsDeletedFalseOrderBySessionDateAsc(
            UUID eventId,
            SessionStatus status
    );

    // Find upcoming sessions
    List<EventSessionEntity> findByEventIdAndSessionDateAfterAndIsDeletedFalseOrderBySessionDateAsc(
            UUID eventId,
            LocalDate after
    );

    // Find session by date
    List<EventSessionEntity> findByEventIdAndSessionDateAndIsDeletedFalse(
            UUID eventId,
            LocalDate sessionDate
    );

    // Count sessions for an event
    long countByEventIdAndIsDeletedFalse(UUID eventId);
}