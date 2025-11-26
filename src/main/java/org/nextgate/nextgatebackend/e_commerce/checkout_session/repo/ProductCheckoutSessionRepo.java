// CheckoutSessionRepository.java
package org.nextgate.nextgatebackend.e_commerce.checkout_session.repo;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.ProductCheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductCheckoutSessionRepo extends JpaRepository<ProductCheckoutSessionEntity, UUID> {

    // Find by session ID and customer (for ownership verification)
    Optional<ProductCheckoutSessionEntity> findBySessionIdAndCustomer(UUID sessionId, AccountEntity customer);

    // Find all sessions for a customer
    List<ProductCheckoutSessionEntity> findByCustomerOrderByCreatedAtDesc(AccountEntity customer);

    // Find active sessions by customer and status
    List<ProductCheckoutSessionEntity> findByCustomerAndStatus(AccountEntity customer, CheckoutSessionStatus status);

    Optional<ProductCheckoutSessionEntity> findBySessionId(UUID sessionId);
}