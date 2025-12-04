package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.repo;

import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.entity.RegistrationTokenEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RegistrationToken entity
 * Contains only essential queries needed for scanner registration
 */
public interface RegistrationTokenRepository extends JpaRepository<RegistrationTokenEntity, UUID> {

    /**
     * Find registration token by token string
     * Used by: Scanner registration validation
     */
    Optional<RegistrationTokenEntity> findByToken(String token);

    /**
     * Find all tokens for a specific event
     * Used by: Admin dashboard to view registration tokens
     */
    List<RegistrationTokenEntity> findByEvent(EventEntity event);

    /**
     * Find all unused, non-expired tokens for an event
     * Used by: Admin dashboard to show active registration tokens
     */
    List<RegistrationTokenEntity> findByEventAndUsedAndExpiresAtAfter(
            EventEntity event,
            Boolean used,
            Instant expiresAt
    );

    /**
     * Delete expired tokens (cleanup)
     * Used by: A scheduled job to clean up old tokens
     */
    void deleteByExpiresAtBefore(Instant expiresAt);
}