package org.nextgate.nextgatebackend.wallet_service.transactions.repo;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.wallet_service.transactions.entity.TransactionEntity;
import org.nextgate.nextgatebackend.wallet_service.transactions.enums.TransactionStatus;
import org.nextgate.nextgatebackend.wallet_service.transactions.enums.TransactionType;
import org.nextgate.nextgatebackend.wallet_service.wallet.entity.WalletEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepo extends JpaRepository<TransactionEntity, UUID> {

    // Find by transaction reference
    Optional<TransactionEntity> findByTransactionRef(String transactionRef);

    // Account-based queries
    List<TransactionEntity> findByAccountOrderByCreatedAtDesc(AccountEntity account);
    Page<TransactionEntity> findByAccountOrderByCreatedAtDesc(AccountEntity account, Pageable pageable);

    // Wallet-based queries
    List<TransactionEntity> findByWallet(WalletEntity wallet);
    Page<TransactionEntity> findByWallet(WalletEntity wallet, Pageable pageable);

    // Wallet transactions only (for balance calculation)
    List<TransactionEntity> findByWalletAndStatusOrderByCreatedAtDesc(WalletEntity wallet, TransactionStatus status);

    // Account wallet transactions (where wallet is not null)
    List<TransactionEntity> findByAccountAndWalletIsNotNullOrderByCreatedAtDesc(AccountEntity account);
    Page<TransactionEntity> findByAccountAndWalletIsNotNullOrderByCreatedAtDesc(AccountEntity account, Pageable pageable);

    // Account non-wallet transactions (where wallet is null)
    List<TransactionEntity> findByAccountAndWalletIsNullOrderByCreatedAtDesc(AccountEntity account);
    Page<TransactionEntity> findByAccountAndWalletIsNullOrderByCreatedAtDesc(AccountEntity account, Pageable pageable);

    // Status-based queries
    List<TransactionEntity> findByAccountAndStatusOrderByCreatedAtDesc(AccountEntity account, TransactionStatus status);

    // Balance calculation - wallet completed transactions
    List<TransactionEntity> findByWalletAndStatusAndTransactionTypeOrderByCreatedAtDesc(
            WalletEntity wallet, TransactionStatus status, TransactionType transactionType);

    // Recent transactions (limited)
    List<TransactionEntity> findTop10ByAccountOrderByCreatedAtDesc(AccountEntity account);
    List<TransactionEntity> findTop10ByWalletOrderByCreatedAtDesc(WalletEntity wallet);

    // Counting methods
    long countByAccountAndStatus(AccountEntity account, TransactionStatus status);
    long countByWalletAndStatus(WalletEntity wallet, TransactionStatus status);

    // Related transactions
    List<TransactionEntity> findByRelatedTransactionIdOrderByCreatedAtDesc(String relatedTransactionId);

    // Existence checks
    boolean existsByTransactionRef(String transactionRef);

    List<TransactionEntity> findByAccount(AccountEntity account);

    // Find transactions by account and date range
    List<TransactionEntity> findByAccountAndCreatedAtBetweenOrderByCreatedAtDesc(
            AccountEntity account,
            LocalDateTime startDate,
            LocalDateTime endDate);

    Page<TransactionEntity> findByAccountAndCreatedAtBetweenOrderByCreatedAtDesc(
            AccountEntity account,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);
}