package org.nextgate.nextgatebackend.financial_system.transaction_history.repo;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.financial_system.transaction_history.entity.TransactionHistory;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionDirection;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionStatus;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionHistoryRepo extends JpaRepository<TransactionHistory, UUID> {

    Optional<TransactionHistory> findByTransactionRef(String transactionRef);

    Page<TransactionHistory> findByAccountOrderByCreatedAtDesc(AccountEntity account, Pageable pageable);

    List<TransactionHistory> findByAccountOrderByCreatedAtDesc(AccountEntity account);

    Page<TransactionHistory> findByAccountAndTypeOrderByCreatedAtDesc(AccountEntity account, TransactionType type, Pageable pageable);

    Page<TransactionHistory> findByAccountAndDirectionOrderByCreatedAtDesc(AccountEntity account, TransactionDirection direction, Pageable pageable);

    Page<TransactionHistory> findByAccountAndCreatedAtBetweenOrderByCreatedAtDesc(AccountEntity account, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    List<TransactionHistory> findByAccountAndCreatedAtBetweenOrderByCreatedAtDesc(AccountEntity account, LocalDateTime startDate, LocalDateTime endDate);

    List<TransactionHistory> findByReferenceTypeAndReferenceId(String referenceType, UUID referenceId);

    long countByAccount(AccountEntity account);

    boolean existsByTransactionRef(String transactionRef);
}