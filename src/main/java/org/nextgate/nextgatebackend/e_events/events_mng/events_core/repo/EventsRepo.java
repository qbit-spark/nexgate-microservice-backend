package org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventsRepo extends JpaRepository<EventEntity, UUID> {

    // Find by ID (not deleted)
    Optional<EventEntity> findByIdAndIsDeletedFalse(UUID id);

    // Check slug existence
    boolean existsBySlugAndIsDeletedFalse(String slug);

    boolean existsBySlugAndIdNotAndIsDeletedFalse(String slug, UUID id);

    // Find similar events by title and date range (for online events)
    List<EventEntity> findByTitleContainingIgnoreCaseAndStartDateTimeBetweenAndStatusAndIsDeletedFalse(
            String title,
            ZonedDateTime searchStart,
            ZonedDateTime searchEnd,
            EventStatus status
    );

    // Add to EventsRepo.java

    List<EventEntity> findByTitleContainingIgnoreCaseAndStartDateTimeBetweenAndStatusInAndIsDeletedFalse(
            String title,
            ZonedDateTime searchStart,
            ZonedDateTime searchEnd,
            List<EventStatus> statuses
    );

    // Rate limiting queries
    long countByOrganizerAndCreatedAtAfter(
            AccountEntity organizer,
            ZonedDateTime after
    );

    long countByOrganizerAndCreatedAtAfterAndIsDeletedFalse(
            AccountEntity organizer,
            ZonedDateTime after
    );

    // Organizer reputation checks
    long countByOrganizer(AccountEntity organizer);

    long countByOrganizerAndStatus(AccountEntity organizer, EventStatus status);

    // Find similar events by organizer and title (recent)
    long countByOrganizerAndTitleContainingIgnoreCaseAndCreatedAtAfter(
            AccountEntity organizer,
            String title,
            ZonedDateTime after
    );

    // Find events by organizer


    // Find events by category
    List<EventEntity> findByCategoryCategoryIdAndIsDeletedFalseAndStatus(
            UUID categoryId,
            EventStatus status
    );

    // Find published events
    List<EventEntity> findByStatusAndIsDeletedFalseOrderByStartDateTimeAsc(
            EventStatus status
    );

    List<EventEntity> findByOrganizerAndIsDeletedFalse(AccountEntity organizer);

    Optional<EventEntity> findByIdAndStatusAndIsDeletedFalse(UUID id, EventStatus eventStatus);

    Page<EventEntity> findByOrganizerAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(
            AccountEntity organizer,
            EventStatus status,
            org.springframework.data.domain.Pageable pageable
    );

    Page<EventEntity> findByOrganizerAndIsDeletedFalseOrderByCreatedAtDesc(
            AccountEntity organizer,
            Pageable pageable
    );
}