package org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.repo;

import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.entity.TicketEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.AttendanceMode;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepo extends JpaRepository<TicketEntity, UUID> {

    // Find by ID (not deleted)
    Optional<TicketEntity> findByIdAndIsDeletedFalse(UUID id);

    // Get all tickets for an event (not deleted)
    List<TicketEntity> findByEventAndIsDeletedFalseOrderByCreatedAtAsc(EventEntity event);

    // Check if ticket name exists for event (uniqueness check)
    boolean existsByEventAndNameAndAttendanceModeAndIsDeletedFalse(
            EventEntity event,
            String name,
            AttendanceMode attendanceMode
    );

    // Check for duplicate when updating (exclude current ticket)
    boolean existsByEventAndNameAndAttendanceModeAndIdNotAndIsDeletedFalse(
            EventEntity event,
            String name,
            AttendanceMode attendanceMode,
            UUID id
    );

    // Count active tickets for event (for publishing validation)
    long countByEventAndStatusAndIsDeletedFalse(
            EventEntity event,
            TicketStatus status
    );

    // Count active tickets by attendance mode (for HYBRID event validation)
    long countByEventAndAttendanceModeAndStatusAndIsDeletedFalse(
            EventEntity event,
            AttendanceMode attendanceMode,
            TicketStatus status
    );

    // Get active tickets for event by attendance mode
    List<TicketEntity> findByEventAndAttendanceModeAndStatusAndIsDeletedFalse(
            EventEntity event,
            AttendanceMode attendanceMode,
            TicketStatus status
    );
}