package org.nextgate.nextgatebackend.financial_system.escrow.repo;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.enums.EscrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EscrowAccountRepo extends JpaRepository<EscrowAccountEntity, UUID> {

    // Find by escrow number
    Optional<EscrowAccountEntity> findByEscrowNumber(String escrowNumber);

    // Find by order ID
    Optional<EscrowAccountEntity> findByOrderId(String orderId);

    // Find all escrows for a buyer
    List<EscrowAccountEntity> findByBuyerOrderByCreatedAtDesc(AccountEntity buyer);

    // Find all escrows for a seller
    List<EscrowAccountEntity> findBySellerOrderByCreatedAtDesc(AccountEntity seller);

    // Find by status
    List<EscrowAccountEntity> findByStatus(EscrowStatus status);

    // Find held escrows (for monitoring)
    List<EscrowAccountEntity> findByStatusOrderByCreatedAtDesc(EscrowStatus status);


    // Check if escrow number exists
    boolean existsByEscrowNumber(String escrowNumber);

    Optional<EscrowAccountEntity> findByCheckoutSessionIdAndSessionDomain(UUID sessionId, String sessionDomain);

    boolean existsByCheckoutSessionIdAndSessionDomain(UUID sessionId, String sessionDomain);
}