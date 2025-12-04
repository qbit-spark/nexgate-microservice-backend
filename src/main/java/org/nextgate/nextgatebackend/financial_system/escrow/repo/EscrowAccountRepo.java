package org.nextgate.nextgatebackend.financial_system.escrow.repo;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.enums.EscrowStatus;
import org.nextgate.nextgatebackend.globe_enums.CheckoutSessionsDomains;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface EscrowAccountRepo extends JpaRepository<EscrowAccountEntity, UUID> {

    Optional<EscrowAccountEntity> findByEscrowNumber(String escrowNumber);

    Optional<EscrowAccountEntity> findByCheckoutSessionIdAndSessionDomain(
            UUID sessionId,
            CheckoutSessionsDomains sessionDomain); // ← Enum

    Optional<EscrowAccountEntity> findByOrderId(String orderId);

    List<EscrowAccountEntity> findByBuyerOrderByCreatedAtDesc(AccountEntity buyer);

    List<EscrowAccountEntity> findBySellerOrderByCreatedAtDesc(AccountEntity seller);

    List<EscrowAccountEntity> findByStatus(EscrowStatus status);

    List<EscrowAccountEntity> findByStatusOrderByCreatedAtDesc(EscrowStatus status);

    boolean existsByCheckoutSessionIdAndSessionDomain(
            UUID sessionId,
            CheckoutSessionsDomains sessionDomain); // ← Enum

    boolean existsByEscrowNumber(String escrowNumber);
}