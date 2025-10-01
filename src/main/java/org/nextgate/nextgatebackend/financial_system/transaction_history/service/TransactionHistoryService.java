package org.nextgate.nextgatebackend.financial_system.transaction_history.service;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.financial_system.transaction_history.entity.TransactionHistory;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionDirection;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionType;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionHistoryService {

    TransactionHistory createTransaction(
            AccountEntity account,
            TransactionType type,
            TransactionDirection direction,
            BigDecimal amount,
            String title,
            String description,
            UUID ledgerEntryId,
            String referenceType,
            UUID referenceId
    );

    TransactionHistory getById(UUID id) throws ItemNotFoundException;

    TransactionHistory getByTransactionRef(String transactionRef) throws ItemNotFoundException;

    Page<TransactionHistory> getMyTransactions(Pageable pageable) throws ItemNotFoundException;

    Page<TransactionHistory> getMyTransactionsByType(TransactionType type, Pageable pageable) throws ItemNotFoundException;

    Page<TransactionHistory> getMyTransactionsByDirection(TransactionDirection direction, Pageable pageable) throws ItemNotFoundException;

    Page<TransactionHistory> getMyTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) throws ItemNotFoundException;

    List<TransactionHistory> getTransactionsByReference(String referenceType, UUID referenceId);

    long getMyTransactionCount() throws ItemNotFoundException;

    String generateTransactionRef();
}