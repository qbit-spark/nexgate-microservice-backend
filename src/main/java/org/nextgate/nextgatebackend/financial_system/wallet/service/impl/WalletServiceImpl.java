package org.nextgate.nextgatebackend.financial_system.wallet.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.financial_system.ledger.entity.LedgerAccountEntity;
import org.nextgate.nextgatebackend.financial_system.ledger.entity.LedgerEntryEntity;
import org.nextgate.nextgatebackend.financial_system.ledger.enums.LedgerEntryType;
import org.nextgate.nextgatebackend.financial_system.ledger.service.LedgerService;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionDirection;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionType;
import org.nextgate.nextgatebackend.financial_system.transaction_history.service.TransactionHistoryService;
import org.nextgate.nextgatebackend.financial_system.wallet.entity.WalletEntity;
import org.nextgate.nextgatebackend.financial_system.wallet.repo.WalletRepository;
import org.nextgate.nextgatebackend.financial_system.wallet.service.WalletService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final AccountRepo accountRepo;
    private final LedgerService ledgerService;
    private final TransactionHistoryService transactionHistoryService;


    @Override
    @Transactional
    public WalletEntity getMyWallet() throws ItemNotFoundException {
        AccountEntity account = getAuthenticatedAccount();
        return initializeWallet(account);
    }

    @Override
    public WalletEntity getWalletById(UUID walletId) throws ItemNotFoundException {
        AccountEntity account = getAuthenticatedAccount();
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ItemNotFoundException("Wallet not found"));

        if (!validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), account, wallet)) {
            throw new ItemNotFoundException("You do not have permission to access this wallet");
        }

        return wallet;
    }

    @Override
    @Transactional
    public WalletEntity getWalletByAccountId(UUID accountId) throws ItemNotFoundException {
        AccountEntity account = accountRepo.findById(accountId)
                .orElseThrow(() -> new ItemNotFoundException("Account not found"));

        WalletEntity wallet = initializeWallet(account);

        AccountEntity authAccount = getAuthenticatedAccount();
        if (!validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), authAccount, wallet)) {
            throw new ItemNotFoundException("You do not have permission to access this wallet");
        }

        return wallet;
    }

    @Override
    public BigDecimal getMyWalletBalance() throws ItemNotFoundException {
        AccountEntity account = getAuthenticatedAccount();
        WalletEntity wallet = initializeWallet(account);
        return getWalletBalance(wallet);
    }

    @Override
    public BigDecimal getWalletBalance(WalletEntity wallet) throws ItemNotFoundException {
        LedgerAccountEntity ledgerAccount = ledgerService.getOrCreateWalletAccount(wallet);
        return ledgerService.getBalance(ledgerAccount);
    }

    @Override
    public LedgerAccountEntity getLedgerAccount(WalletEntity wallet) throws ItemNotFoundException {
        return ledgerService.getOrCreateWalletAccount(wallet);
    }

    @Override
    @Transactional
    public void activateWallet(UUID walletId) throws ItemNotFoundException {
        AccountEntity account = getAuthenticatedAccount();
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ItemNotFoundException("Wallet not found"));

        if (!validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN"), account, wallet)) {
            throw new ItemNotFoundException("You do not have permission to activate this wallet");
        }

        wallet.activate();
        walletRepository.save(wallet);

        log.info("Wallet activated: {} by user: {}", walletId, account.getUserName());
    }

    @Override
    @Transactional
    public void deactivateWallet(UUID walletId, String reason) throws ItemNotFoundException {
        AccountEntity account = getAuthenticatedAccount();
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ItemNotFoundException("Wallet not found"));

        if (!validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), account, wallet)) {
            throw new ItemNotFoundException("You do not have permission to deactivate this wallet");
        }

        wallet.deactivate(account.getAccountId(), reason);
        walletRepository.save(wallet);

        log.info("Wallet deactivated: {} by user: {} - Reason: {}", walletId, account.getUserName(), reason);
    }

    @Override
    @Transactional
    public WalletEntity topupWallet(BigDecimal amount, String description)
            throws ItemNotFoundException, RandomExceptions {

        AccountEntity account = getAuthenticatedAccount();
        WalletEntity wallet = initializeWallet(account);

        if (!wallet.getIsActive()) {
            throw new RandomExceptions("Wallet is not active. Please contact support.");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RandomExceptions("Topup amount must be greater than zero");
        }

        LedgerAccountEntity walletLedgerAccount = ledgerService.getOrCreateWalletAccount(wallet);
        LedgerAccountEntity externalMoneyIn = ledgerService.getExternalMoneyInAccount();

        // Create ledger entry
        LedgerEntryEntity ledgerEntry = ledgerService.createEntry(
                externalMoneyIn,
                walletLedgerAccount,
                amount,
                LedgerEntryType.WALLET_TOPUP,
                "WALLET",
                wallet.getId(),
                description,
                account
        );

        // Create transaction history
        transactionHistoryService.createTransaction(
                account,
                TransactionType.WALLET_TOPUP,
                TransactionDirection.CREDIT,
                amount,
                "Wallet Topup",
                description != null ? description : "Added funds to wallet",
                ledgerEntry.getId(),
                "WALLET",
                wallet.getId()
        );

        wallet.recordActivity();
        walletRepository.save(wallet);

        log.info("Wallet topup: {} TZS for user: {}", amount, account.getUserName());

        return wallet;
    }


    @Override
    @Transactional
    public WalletEntity withdrawFromWallet(BigDecimal amount, String description)
            throws ItemNotFoundException, RandomExceptions {

        AccountEntity account = getAuthenticatedAccount();
        WalletEntity wallet = initializeWallet(account);

        if (!wallet.getIsActive()) {
            throw new RandomExceptions("Wallet is not active. Please contact support.");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RandomExceptions("Withdrawal amount must be greater than zero");
        }

        LedgerAccountEntity walletLedgerAccount = ledgerService.getOrCreateWalletAccount(wallet);

        if (!ledgerService.hasSufficientBalance(walletLedgerAccount, amount)) {
            BigDecimal currentBalance = ledgerService.getBalance(walletLedgerAccount);
            throw new RandomExceptions(
                    String.format("Insufficient balance. Required: %s TZS, Available: %s TZS",
                            amount, currentBalance)
            );
        }

        LedgerAccountEntity externalMoneyOut = ledgerService.getExternalMoneyOutAccount();

        // Create ledger entry
        LedgerEntryEntity ledgerEntry = ledgerService.createEntry(
                walletLedgerAccount,
                externalMoneyOut,
                amount,
                LedgerEntryType.WALLET_WITHDRAWAL,
                "WALLET",
                wallet.getId(),
                description,
                account
        );

        // Create transaction history
        transactionHistoryService.createTransaction(
                account,
                TransactionType.WALLET_WITHDRAWAL,
                TransactionDirection.DEBIT,
                amount,
                "Wallet Withdrawal",
                description != null ? description : "Withdrew funds from wallet",
                ledgerEntry.getId(),
                "WALLET",
                wallet.getId()
        );

        wallet.recordActivity();
        walletRepository.save(wallet);

        log.info("Wallet withdrawal: {} TZS for user: {}", amount, account.getUserName());

        return wallet;
    }


    @Override
    public boolean hasSufficientBalance(WalletEntity wallet, BigDecimal amount)
            throws ItemNotFoundException {

        LedgerAccountEntity ledgerAccount = ledgerService.getOrCreateWalletAccount(wallet);
        return ledgerService.hasSufficientBalance(ledgerAccount, amount);
    }

    @Override
    @Transactional
    public void recordActivity(WalletEntity wallet) {
        wallet.recordActivity();
        walletRepository.save(wallet);
    }

    private WalletEntity initializeWallet(AccountEntity account) {
        return walletRepository.findByAccount(account)
                .orElseGet(() -> {
                    WalletEntity wallet = WalletEntity.builder()
                            .account(account)
                            .isActive(true)
                            .build();
                    return walletRepository.save(wallet);
                });
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

    private boolean validateSystemRolesOrOwner(List<String> customRoles, AccountEntity account, WalletEntity wallet) {
        boolean hasCustomRole = account.getRoles().stream()
                .anyMatch(role -> customRoles.contains(role.getRoleName()));

        boolean isOwner = wallet.getAccount().getAccountId().equals(account.getAccountId());

        return hasCustomRole || isOwner;
    }
}