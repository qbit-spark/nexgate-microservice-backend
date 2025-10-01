package org.nextgate.nextgatebackend.financial_system.ledger.service;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.financial_system.ledger.entity.LedgerAccountEntity;
import org.nextgate.nextgatebackend.financial_system.ledger.entity.LedgerEntryEntity;
import org.nextgate.nextgatebackend.financial_system.ledger.enums.LedgerAccountType;
import org.nextgate.nextgatebackend.financial_system.ledger.enums.LedgerEntryType;
import org.nextgate.nextgatebackend.financial_system.wallet.entity.WalletEntity;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core Ledger Service - Double-Entry Bookkeeping System
 * All money movements in the system must go through this service
 */
public interface LedgerService {

    // Core method: Creates a double-entry ledger transaction (debit one account, credit another)
    LedgerEntryEntity createEntry(
            LedgerAccountEntity debitAccount,
            LedgerAccountEntity creditAccount,
            BigDecimal amount,
            LedgerEntryType entryType,
            String referenceType,
            UUID referenceId,
            String description,
            AccountEntity createdBy
    );

    // Creates a double-entry with additional metadata stored as JSON
    LedgerEntryEntity createEntryWithMetadata(
            LedgerAccountEntity debitAccount,
            LedgerAccountEntity creditAccount,
            BigDecimal amount,
            LedgerEntryType entryType,
            String referenceType,
            UUID referenceId,
            String description,
            Map<String, Object> metadata,
            AccountEntity createdBy
    );

    // Splits money from one account to multiple accounts (used for escrow release: seller + platform fee)
    List<LedgerEntryEntity> createSplitEntry(
            LedgerAccountEntity debitAccount,
            Map<LedgerAccountEntity, BigDecimal> creditAccounts,
            LedgerEntryType entryType,
            String referenceType,
            UUID referenceId,
            String description,
            AccountEntity createdBy
    );

    // Gets existing ledger account for a wallet, or creates one if doesn't exist
    LedgerAccountEntity getOrCreateWalletAccount(WalletEntity wallet);

    // Creates a new ledger account specifically for an escrow transaction
    LedgerAccountEntity createEscrowAccount(UUID escrowReferenceId);

    // Gets the singleton platform revenue account (where all fees are collected)
    LedgerAccountEntity getPlatformRevenueAccount();

    // Gets the singleton platform reserve account (emergency fund)
    LedgerAccountEntity getPlatformReserveAccount();

    // Gets the virtual account representing money coming from external sources (M-Pesa, cards, etc.)
    LedgerAccountEntity getExternalMoneyInAccount();

    // Gets the virtual account representing money going to external destinations (bank withdrawals)
    LedgerAccountEntity getExternalMoneyOutAccount();

    // Finds a ledger account by its unique account number
    LedgerAccountEntity getAccountByNumber(String accountNumber) throws ItemNotFoundException;

    // Finds a ledger account by its UUID
    LedgerAccountEntity getAccountById(UUID accountId) throws ItemNotFoundException;

    // Gets all ledger accounts owned by a specific user
    List<LedgerAccountEntity> getUserAccounts(AccountEntity owner);

    // Returns the current cached balance from the account (fast)
    BigDecimal getBalance(LedgerAccountEntity account);

    // Calculates balance by summing all ledger entries (slow, used for verification)
    BigDecimal calculateBalanceFromEntries(LedgerAccountEntity account);

    // Checks if account has enough money for a transaction
    boolean hasSufficientBalance(LedgerAccountEntity account, BigDecimal amount);

    // Sums up balances of all accounts of a specific type (for reporting)
    BigDecimal getTotalBalanceByType(LedgerAccountType accountType);

    // Gets all ledger entries where this account was debited or credited
    List<LedgerEntryEntity> getAccountEntries(LedgerAccountEntity account);

    // Gets all ledger entries related to a specific entity (e.g., all entries for an order)
    List<LedgerEntryEntity> getEntriesByReference(String referenceType, UUID referenceId);

    // Finds a specific ledger entry by its unique entry number
    LedgerEntryEntity getEntryByNumber(String entryNumber) throws ItemNotFoundException;

    // Validates that a proposed entry is valid before creating it
    void validateEntry(LedgerAccountEntity debitAccount, LedgerAccountEntity creditAccount, BigDecimal amount);

    // Generates a unique entry number in format "LE-YYYY-NNNNNN"
    String generateEntryNumber();

    // Generates a unique account number based on account type and identifier
    String generateAccountNumber(LedgerAccountType accountType, String identifier);

    // Verifies that sum of all debits equals sum of all credits (fundamental accounting rule)
    boolean verifyLedgerBalance();

    // Calculates total transaction volume between two dates
    BigDecimal getTransactionVolume(LocalDateTime startDate, LocalDateTime endDate);

    // Returns total count of all ledger entries in the system
    long getTotalEntryCount();

    // Returns the most recent ledger entries (for dashboard display)
    List<LedgerEntryEntity> getRecentEntries(int limit);
}