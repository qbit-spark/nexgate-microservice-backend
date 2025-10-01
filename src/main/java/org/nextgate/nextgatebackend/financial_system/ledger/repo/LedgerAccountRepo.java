package org.nextgate.nextgatebackend.financial_system.ledger.repo;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.financial_system.ledger.entity.LedgerAccountEntity;
import org.nextgate.nextgatebackend.financial_system.ledger.enums.LedgerAccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerAccountRepo extends JpaRepository<LedgerAccountEntity, UUID> {

    // Find by account number
    Optional<LedgerAccountEntity> findByAccountNumber(String accountNumber);

    // Find by owner and account type (for user wallets)
    Optional<LedgerAccountEntity> findByOwnerAndAccountType(AccountEntity owner, LedgerAccountType accountType);

    // Find by account type (for platform accounts)
    Optional<LedgerAccountEntity> findByAccountType(LedgerAccountType accountType);

    // Find by escrow reference
    Optional<LedgerAccountEntity> findByEscrowReferenceId(UUID escrowReferenceId);

    // Find all accounts by owner
    List<LedgerAccountEntity> findByOwner(AccountEntity owner);

    // Find all active accounts of a type
    List<LedgerAccountEntity> findByAccountTypeAndIsActiveTrue(LedgerAccountType accountType);

    // Find accounts with positive balance
    List<LedgerAccountEntity> findByAccountTypeAndCurrentBalanceGreaterThanAndIsActiveTrue(
            LedgerAccountType accountType,
            BigDecimal balance
    );

    // Check if account number exists
    boolean existsByAccountNumber(String accountNumber);
}