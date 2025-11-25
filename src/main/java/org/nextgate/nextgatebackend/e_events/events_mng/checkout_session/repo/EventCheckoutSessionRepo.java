package org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.repo;


import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.entity.EventCheckoutSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for EventCheckoutSession
 * Contains only essential query methods
 */
@Repository
public interface EventCheckoutSessionRepo extends JpaRepository<EventCheckoutSessionEntity, UUID> {

    // Find by session ID and customer (ownership verification)
    Optional<EventCheckoutSessionEntity> findBySessionIdAndCustomer(UUID sessionId, AccountEntity customer);

    // Find all sessions for a customer
    List<EventCheckoutSessionEntity> findByCustomerOrderByCreatedAtDesc(AccountEntity customer);

    // Find sessions by customer and status
    List<EventCheckoutSessionEntity> findByCustomerAndStatus(AccountEntity customer, CheckoutSessionStatus status);

    // Find expired pending sessions (for ticket hold release job)
    List<EventCheckoutSessionEntity> findByStatusAndExpiresAtBefore(
            CheckoutSessionStatus status,
            LocalDateTime expirationTime
    );
}