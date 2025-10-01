package org.nextgate.nextgatebackend.financial_system.transaction_history.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.financial_system.transaction_history.entity.TransactionHistory;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionDirection;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionStatus;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionType;
import org.nextgate.nextgatebackend.financial_system.transaction_history.repo.TransactionHistoryRepo;
import org.nextgate.nextgatebackend.financial_system.transaction_history.service.TransactionHistoryService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionHistoryServiceImpl implements TransactionHistoryService {

    private final TransactionHistoryRepo transactionHistoryRepo;
    private final AccountRepo accountRepo;

    @Override
    @Transactional
    public TransactionHistory createTransaction(
            AccountEntity account,
            TransactionType type,
            TransactionDirection direction,
            BigDecimal amount,
            String title,
            String description,
            UUID ledgerEntryId,
            String referenceType,
            UUID referenceId) {

        String transactionRef = generateTransactionRef();

        TransactionHistory transaction = TransactionHistory.builder()
                .transactionRef(transactionRef)
                .account(account)
                .type(type)
                .direction(direction)
                .amount(amount)
                .title(title)
                .description(description)
                .ledgerEntryId(ledgerEntryId)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .status(TransactionStatus.COMPLETED)
                .build();

        TransactionHistory saved = transactionHistoryRepo.save(transaction);

        log.info("Transaction history created: {} - {} {} TZS for user: {}",
                transactionRef, direction, amount, account.getUserName());

        return saved;
    }

    @Override
    public TransactionHistory getById(UUID id) throws ItemNotFoundException {
        return transactionHistoryRepo.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Transaction not found"));
    }

    @Override
    public TransactionHistory getByTransactionRef(String transactionRef) throws ItemNotFoundException {
        return transactionHistoryRepo.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new ItemNotFoundException("Transaction not found: " + transactionRef));
    }

    @Override
    public Page<TransactionHistory> getMyTransactions(Pageable pageable) throws ItemNotFoundException {
        AccountEntity account = getAuthenticatedAccount();
        return transactionHistoryRepo.findByAccountOrderByCreatedAtDesc(account, pageable);
    }

    @Override
    public Page<TransactionHistory> getMyTransactionsByType(TransactionType type, Pageable pageable)
            throws ItemNotFoundException {
        AccountEntity account = getAuthenticatedAccount();
        return transactionHistoryRepo.findByAccountAndTypeOrderByCreatedAtDesc(account, type, pageable);
    }

    @Override
    public Page<TransactionHistory> getMyTransactionsByDirection(TransactionDirection direction, Pageable pageable)
            throws ItemNotFoundException {
        AccountEntity account = getAuthenticatedAccount();
        return transactionHistoryRepo.findByAccountAndDirectionOrderByCreatedAtDesc(account, direction, pageable);
    }

    @Override
    public Page<TransactionHistory> getMyTransactionsByDateRange(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) throws ItemNotFoundException {

        AccountEntity account = getAuthenticatedAccount();
        return transactionHistoryRepo.findByAccountAndCreatedAtBetweenOrderByCreatedAtDesc(
                account, startDate, endDate, pageable);
    }

    @Override
    public List<TransactionHistory> getTransactionsByReference(String referenceType, UUID referenceId) {
        return transactionHistoryRepo.findByReferenceTypeAndReferenceId(referenceType, referenceId);
    }

    @Override
    public long getMyTransactionCount() throws ItemNotFoundException {
        AccountEntity account = getAuthenticatedAccount();
        return transactionHistoryRepo.countByAccount(account);
    }

    @Override
    public String generateTransactionRef() {
        int year = Year.now().getValue();
        long count = transactionHistoryRepo.count() + 1;
        String ref = String.format("#%dT%06d", year, count);

        while (transactionHistoryRepo.existsByTransactionRef(ref)) {
            count++;
            ref = String.format("#%dT%06d", year, count);
        }

        return ref;
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
        }
        throw new ItemNotFoundException("User not authenticated");
    }
}