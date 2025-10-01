package org.nextgate.nextgatebackend.financial_system.ledger.repo;

import org.nextgate.nextgatebackend.financial_system.ledger.entity.LedgerAccountEntity;
import org.nextgate.nextgatebackend.financial_system.ledger.entity.LedgerEntryEntity;
import org.nextgate.nextgatebackend.financial_system.ledger.enums.LedgerEntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerEntryRepo extends JpaRepository<LedgerEntryEntity, UUID> {

    // Find by entry number
    Optional<LedgerEntryEntity> findByEntryNumber(String entryNumber);

    // Find entries where account is debit
    List<LedgerEntryEntity> findByDebitAccountOrderByCreatedAtDesc(LedgerAccountEntity account);

    // Find entries where account is credit
    List<LedgerEntryEntity> findByCreditAccountOrderByCreatedAtDesc(LedgerAccountEntity account);

    // Find entries by reference
    List<LedgerEntryEntity> findByReferenceTypeAndReferenceId(String referenceType, UUID referenceId);

    // Find entries by type
    List<LedgerEntryEntity> findByEntryType(LedgerEntryType entryType);

    // Find entries in date range
    List<LedgerEntryEntity> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    // Check if entry number exists
    boolean existsByEntryNumber(String entryNumber);

    // Find latest entries
    List<LedgerEntryEntity> findTop10ByOrderByCreatedAtDesc();

    // Count entries by account (debit or credit)
    long countByDebitAccountOrCreditAccount(LedgerAccountEntity debitAccount, LedgerAccountEntity creditAccount);
}