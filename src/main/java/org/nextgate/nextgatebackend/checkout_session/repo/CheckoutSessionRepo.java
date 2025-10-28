// CheckoutSessionRepository.java
package org.nextgate.nextgatebackend.checkout_session.repo;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckoutSessionRepo extends JpaRepository<CheckoutSessionEntity, UUID> {

    // Find by session ID and customer (for ownership verification)
    Optional<CheckoutSessionEntity> findBySessionIdAndCustomer(UUID sessionId, AccountEntity customer);

    // Find all sessions for a customer
    List<CheckoutSessionEntity> findByCustomerOrderByCreatedAtDesc(AccountEntity customer);

    // Find active sessions by customer and status
    List<CheckoutSessionEntity> findByCustomerAndStatus(AccountEntity customer, CheckoutSessionStatus status);

    Optional<CheckoutSessionEntity> findBySessionId(UUID sessionId);
}