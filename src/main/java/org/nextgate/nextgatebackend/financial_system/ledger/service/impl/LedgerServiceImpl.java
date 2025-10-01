package org.nextgate.nextgatebackend.financial_system.ledger.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.financial_system.ledger.entity.LedgerAccountEntity;
import org.nextgate.nextgatebackend.financial_system.ledger.entity.LedgerEntryEntity;
import org.nextgate.nextgatebackend.financial_system.ledger.enums.LedgerAccountType;
import org.nextgate.nextgatebackend.financial_system.ledger.enums.LedgerEntryType;
import org.nextgate.nextgatebackend.financial_system.ledger.repo.LedgerAccountRepo;
import org.nextgate.nextgatebackend.financial_system.ledger.repo.LedgerEntryRepo;
import org.nextgate.nextgatebackend.financial_system.ledger.service.LedgerService;
import org.nextgate.nextgatebackend.financial_system.wallet.entity.WalletEntity;
import org.nextgate.nextgatebackend.globeadvice.exceptions.InsufficientBalanceException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.LedgerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerServiceImpl implements LedgerService {

    private final LedgerAccountRepo ledgerAccountRepo;
    private final LedgerEntryRepo ledgerEntryRepo;

    @Value("${app.platform.currency:TZS}")
    private String currency;

    @Override
    @Transactional
    public LedgerEntryEntity createEntry(
            LedgerAccountEntity debitAccount,
            LedgerAccountEntity creditAccount,
            BigDecimal amount,
            LedgerEntryType entryType,
            String referenceType,
            UUID referenceId,
            String description,
            AccountEntity createdBy) {

        validateEntry(debitAccount, creditAccount, amount);

        if (!debitAccount.isExternalAccount() && !debitAccount.canDebit(amount)) {
            throw new InsufficientBalanceException(
                    debitAccount.getAccountNumber(),
                    amount,
                    debitAccount.getCurrentBalance()
            );
        }

        String entryNumber = generateEntryNumber();

        LedgerEntryEntity entry = LedgerEntryEntity.builder()
                .entryNumber(entryNumber)
                .debitAccount(debitAccount)
                .creditAccount(creditAccount)
                .amount(amount)
                .entryType(entryType)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .description(description)
                .currency(currency)
                .createdBy(createdBy)
                .build();

        LedgerEntryEntity savedEntry = ledgerEntryRepo.save(entry);

        updateAccountBalances(debitAccount, creditAccount, amount);

        log.info("Created ledger entry: {} - {} {} from {} to {}",
                entryNumber, amount, currency,
                debitAccount.getAccountNumber(),
                creditAccount.getAccountNumber());

        return savedEntry;
    }

    @Override
    @Transactional
    public LedgerEntryEntity createEntryWithMetadata(
            LedgerAccountEntity debitAccount,
            LedgerAccountEntity creditAccount,
            BigDecimal amount,
            LedgerEntryType entryType,
            String referenceType,
            UUID referenceId,
            String description,
            Map<String, Object> metadata,
            AccountEntity createdBy) {

        validateEntry(debitAccount, creditAccount, amount);

        if (!debitAccount.isExternalAccount() && !debitAccount.canDebit(amount)) {
            throw new InsufficientBalanceException(
                    debitAccount.getAccountNumber(),
                    amount,
                    debitAccount.getCurrentBalance()
            );
        }

        String entryNumber = generateEntryNumber();

        LedgerEntryEntity entry = LedgerEntryEntity.builder()
                .entryNumber(entryNumber)
                .debitAccount(debitAccount)
                .creditAccount(creditAccount)
                .amount(amount)
                .entryType(entryType)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .description(description)
                .metadata(metadata)
                .currency(currency)
                .createdBy(createdBy)
                .build();

        LedgerEntryEntity savedEntry = ledgerEntryRepo.save(entry);
        updateAccountBalances(debitAccount, creditAccount, amount);

        log.info("Created ledger entry with metadata: {}", entryNumber);

        return savedEntry;
    }

    @Override
    @Transactional
    public List<LedgerEntryEntity> createSplitEntry(
            LedgerAccountEntity debitAccount,
            Map<LedgerAccountEntity, BigDecimal> creditAccounts,
            LedgerEntryType entryType,
            String referenceType,
            UUID referenceId,
            String description,
            AccountEntity createdBy) {

        BigDecimal totalAmount = creditAccounts.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!debitAccount.canDebit(totalAmount)) {
            throw new InsufficientBalanceException(
                    debitAccount.getAccountNumber(),
                    totalAmount,
                    debitAccount.getCurrentBalance()
            );
        }

        List<LedgerEntryEntity> entries = new ArrayList<>();

        for (Map.Entry<LedgerAccountEntity, BigDecimal> credit : creditAccounts.entrySet()) {
            LedgerAccountEntity creditAccount = credit.getKey();
            BigDecimal amount = credit.getValue();

            LedgerEntryEntity entry = createEntry(
                    debitAccount,
                    creditAccount,
                    amount,
                    entryType,
                    referenceType,
                    referenceId,
                    description,
                    createdBy
            );

            entries.add(entry);
        }

        log.info("Created split entry: {} debited {} to {} accounts",
                debitAccount.getAccountNumber(), totalAmount, creditAccounts.size());

        return entries;
    }

    @Override
    @Transactional
    public LedgerAccountEntity getOrCreateWalletAccount(WalletEntity wallet) {
        return ledgerAccountRepo
                .findByOwnerAndAccountType(wallet.getAccount(), LedgerAccountType.USER_WALLET)
                .orElseGet(() -> {
                    String accountNumber = generateAccountNumber(
                            LedgerAccountType.USER_WALLET,
                            wallet.getAccount().getUserName()
                    );

                    LedgerAccountEntity account = LedgerAccountEntity.builder()
                            .accountNumber(accountNumber)
                            .accountType(LedgerAccountType.USER_WALLET)
                            .owner(wallet.getAccount())
                            .currentBalance(BigDecimal.ZERO)
                            .currency(currency)
                            .isActive(true)
                            .build();

                    LedgerAccountEntity saved = ledgerAccountRepo.save(account);
                    log.info("Created wallet ledger account: {}", accountNumber);
                    return saved;
                });
    }

    @Override
    @Transactional
    public LedgerAccountEntity createEscrowAccount(UUID escrowReferenceId) {
        String accountNumber = generateAccountNumber(
                LedgerAccountType.ESCROW,
                escrowReferenceId.toString().substring(0, 8)
        );

        LedgerAccountEntity account = LedgerAccountEntity.builder()
                .accountNumber(accountNumber)
                .accountType(LedgerAccountType.ESCROW)
                .escrowReferenceId(escrowReferenceId)
                .currentBalance(BigDecimal.ZERO)
                .currency(currency)
                .isActive(true)
                .build();

        LedgerAccountEntity saved = ledgerAccountRepo.save(account);
        log.info("Created escrow ledger account: {}", accountNumber);
        return saved;
    }

    @Override
    @Transactional
    public LedgerAccountEntity getPlatformRevenueAccount() {
        return ledgerAccountRepo.findByAccountType(LedgerAccountType.PLATFORM_REVENUE)
                .orElseGet(() -> {
                    LedgerAccountEntity account = LedgerAccountEntity.builder()
                            .accountNumber("PLATFORM-REVENUE")
                            .accountType(LedgerAccountType.PLATFORM_REVENUE)
                            .currentBalance(BigDecimal.ZERO)
                            .currency(currency)
                            .isActive(true)
                            .notes("Platform fee collection account")
                            .build();

                    LedgerAccountEntity saved = ledgerAccountRepo.save(account);
                    log.info("Created platform revenue account");
                    return saved;
                });
    }

    @Override
    @Transactional
    public LedgerAccountEntity getPlatformReserveAccount() {
        return ledgerAccountRepo.findByAccountType(LedgerAccountType.PLATFORM_RESERVE)
                .orElseGet(() -> {
                    LedgerAccountEntity account = LedgerAccountEntity.builder()
                            .accountNumber("PLATFORM-RESERVE")
                            .accountType(LedgerAccountType.PLATFORM_RESERVE)
                            .currentBalance(BigDecimal.ZERO)
                            .currency(currency)
                            .isActive(true)
                            .notes("Platform reserve fund")
                            .build();

                    LedgerAccountEntity saved = ledgerAccountRepo.save(account);
                    log.info("Created platform reserve account");
                    return saved;
                });
    }

    @Override
    @Transactional
    public LedgerAccountEntity getExternalMoneyInAccount() {
        return ledgerAccountRepo.findByAccountType(LedgerAccountType.EXTERNAL_MONEY_IN)
                .orElseGet(() -> {
                    LedgerAccountEntity account = LedgerAccountEntity.builder()
                            .accountNumber("EXTERNAL-MONEY-IN")
                            .accountType(LedgerAccountType.EXTERNAL_MONEY_IN)
                            .currentBalance(BigDecimal.ZERO)
                            .currency(currency)
                            .isActive(true)
                            .notes("Virtual account for external payments")
                            .build();

                    LedgerAccountEntity saved = ledgerAccountRepo.save(account);
                    log.info("Created external money in account");
                    return saved;
                });
    }

    @Override
    @Transactional
    public LedgerAccountEntity getExternalMoneyOutAccount() {
        return ledgerAccountRepo.findByAccountType(LedgerAccountType.EXTERNAL_MONEY_OUT)
                .orElseGet(() -> {
                    LedgerAccountEntity account = LedgerAccountEntity.builder()
                            .accountNumber("EXTERNAL-MONEY-OUT")
                            .accountType(LedgerAccountType.EXTERNAL_MONEY_OUT)
                            .currentBalance(BigDecimal.ZERO)
                            .currency(currency)
                            .isActive(true)
                            .notes("Virtual account for withdrawals")
                            .build();

                    LedgerAccountEntity saved = ledgerAccountRepo.save(account);
                    log.info("Created external money out account");
                    return saved;
                });
    }

    @Override
    public LedgerAccountEntity getAccountByNumber(String accountNumber) throws ItemNotFoundException {
        return ledgerAccountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Ledger account not found: " + accountNumber
                ));
    }

    @Override
    public LedgerAccountEntity getAccountById(UUID accountId) throws ItemNotFoundException {
        return ledgerAccountRepo.findById(accountId)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Ledger account not found with ID: " + accountId
                ));
    }

    @Override
    public List<LedgerAccountEntity> getUserAccounts(AccountEntity owner) {
        return ledgerAccountRepo.findByOwner(owner);
    }

    @Override
    public BigDecimal getBalance(LedgerAccountEntity account) {
        return account.getCurrentBalance();
    }

    @Override
    public BigDecimal calculateBalanceFromEntries(LedgerAccountEntity account) {
        List<LedgerEntryEntity> debits = ledgerEntryRepo.findByDebitAccountOrderByCreatedAtDesc(account);
        List<LedgerEntryEntity> credits = ledgerEntryRepo.findByCreditAccountOrderByCreatedAtDesc(account);

        BigDecimal totalCredits = credits.stream()
                .map(LedgerEntryEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebits = debits.stream()
                .map(LedgerEntryEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalCredits.subtract(totalDebits);
    }

    @Override
    public boolean hasSufficientBalance(LedgerAccountEntity account, BigDecimal amount) {
        return account.getCurrentBalance().compareTo(amount) >= 0;
    }

    @Override
    public BigDecimal getTotalBalanceByType(LedgerAccountType accountType) {
        List<LedgerAccountEntity> accounts = ledgerAccountRepo
                .findByAccountTypeAndIsActiveTrue(accountType);

        return accounts.stream()
                .map(LedgerAccountEntity::getCurrentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public List<LedgerEntryEntity> getAccountEntries(LedgerAccountEntity account) {
        List<LedgerEntryEntity> debits = ledgerEntryRepo.findByDebitAccountOrderByCreatedAtDesc(account);
        List<LedgerEntryEntity> credits = ledgerEntryRepo.findByCreditAccountOrderByCreatedAtDesc(account);

        List<LedgerEntryEntity> allEntries = new ArrayList<>(debits);
        allEntries.addAll(credits);

        allEntries.sort((e1, e2) -> e2.getCreatedAt().compareTo(e1.getCreatedAt()));

        return allEntries;
    }

    @Override
    public List<LedgerEntryEntity> getEntriesByReference(String referenceType, UUID referenceId) {
        return ledgerEntryRepo.findByReferenceTypeAndReferenceId(referenceType, referenceId);
    }

    @Override
    public LedgerEntryEntity getEntryByNumber(String entryNumber) throws ItemNotFoundException {
        return ledgerEntryRepo.findByEntryNumber(entryNumber)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Ledger entry not found: " + entryNumber
                ));
    }

    @Override
    public void validateEntry(
            LedgerAccountEntity debitAccount,
            LedgerAccountEntity creditAccount,
            BigDecimal amount) {

        if (debitAccount == null || creditAccount == null) {
            throw new LedgerException("Debit and credit accounts cannot be null");
        }

        if (debitAccount.getId().equals(creditAccount.getId())) {
            throw new LedgerException("Debit and credit accounts must be different");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new LedgerException("Amount must be positive");
        }

        if (!debitAccount.getCurrency().equals(creditAccount.getCurrency())) {
            throw new LedgerException("Account currencies must match");
        }

        if (!debitAccount.getIsActive() || !creditAccount.getIsActive()) {
            throw new LedgerException("Both accounts must be active");
        }
    }

    @Override
    public String generateEntryNumber() {
        int year = Year.now().getValue();
        long count = ledgerEntryRepo.count() + 1;
        String entryNumber = String.format("LE-%d-%06d", year, count);

        while (ledgerEntryRepo.existsByEntryNumber(entryNumber)) {
            count++;
            entryNumber = String.format("LE-%d-%06d", year, count);
        }

        return entryNumber;
    }

    @Override
    public String generateAccountNumber(LedgerAccountType accountType, String identifier) {
        String prefix = switch (accountType) {
            case USER_WALLET -> "WALLET";
            case ESCROW -> "ESCROW";
            case PLATFORM_REVENUE -> "PLATFORM-REV";
            case PLATFORM_RESERVE -> "PLATFORM-RES";
            case EXTERNAL_MONEY_IN -> "EXT-IN";
            case EXTERNAL_MONEY_OUT -> "EXT-OUT";
        };

        String accountNumber = prefix + "-" + identifier;

        int suffix = 1;
        while (ledgerAccountRepo.existsByAccountNumber(accountNumber)) {
            accountNumber = prefix + "-" + identifier + "-" + suffix;
            suffix++;
        }

        return accountNumber;
    }

    @Override
    public boolean verifyLedgerBalance() {
        List<LedgerEntryEntity> allEntries = ledgerEntryRepo.findAll();

        BigDecimal totalDebits = allEntries.stream()
                .map(LedgerEntryEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = allEntries.stream()
                .map(LedgerEntryEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean isBalanced = totalDebits.compareTo(totalCredits) == 0;

        if (!isBalanced) {
            log.error("CRITICAL: Ledger is out of balance! Debits: {}, Credits: {}",
                    totalDebits, totalCredits);
        }

        return isBalanced;
    }

    @Override
    public BigDecimal getTransactionVolume(LocalDateTime startDate, LocalDateTime endDate) {
        List<LedgerEntryEntity> entries = ledgerEntryRepo
                .findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate);

        return entries.stream()
                .map(LedgerEntryEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public long getTotalEntryCount() {
        return ledgerEntryRepo.count();
    }

    @Override
    public List<LedgerEntryEntity> getRecentEntries(int limit) {
        return ledgerEntryRepo.findTop10ByOrderByCreatedAtDesc();
    }


    private void updateAccountBalances(
            LedgerAccountEntity debitAccount,
            LedgerAccountEntity creditAccount,
            BigDecimal amount) {

        debitAccount.setCurrentBalance(debitAccount.getCurrentBalance().subtract(amount));
        debitAccount.setUpdatedAt(LocalDateTime.now());
        ledgerAccountRepo.save(debitAccount);

        creditAccount.setCurrentBalance(creditAccount.getCurrentBalance().add(amount));
        creditAccount.setUpdatedAt(LocalDateTime.now());
        ledgerAccountRepo.save(creditAccount);
    }
}