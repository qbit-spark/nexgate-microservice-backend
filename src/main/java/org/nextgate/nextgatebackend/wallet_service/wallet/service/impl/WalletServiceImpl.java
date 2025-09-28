package org.nextgate.nextgatebackend.wallet_service.wallet.service.impl;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.wallet_service.transactions.entity.TransactionEntity;
import org.nextgate.nextgatebackend.wallet_service.transactions.enums.TransactionStatus;
import org.nextgate.nextgatebackend.wallet_service.transactions.enums.TransactionType;
import org.nextgate.nextgatebackend.wallet_service.transactions.repo.TransactionRepo;
import org.nextgate.nextgatebackend.wallet_service.transactions.service.TransactionService;
import org.nextgate.nextgatebackend.wallet_service.wallet.entity.WalletEntity;
import org.nextgate.nextgatebackend.wallet_service.wallet.repo.WalletRepository;
import org.nextgate.nextgatebackend.wallet_service.wallet.service.WalletService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.nextgate.nextgatebackend.wallet_service.transactions.enums.TransactionType.*;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final AccountRepo accountRepo;
    private final TransactionRepo transactionRepo;
    private final TransactionService transactionService;

    public WalletEntity initializeWallet(AccountEntity account) {
        //Check if wallet already exists for the account, if not create one
        return walletRepository.findByAccount(account)
                .orElseGet(() -> {
                    WalletEntity wallet = WalletEntity.builder()
                            .account(account)
                            .build();
                    return walletRepository.save(wallet);
                });
    }

    @Override
    public WalletEntity getMyWallet() throws ItemNotFoundException {

        AccountEntity account = getAuthenticatedAccount();

        return initializeWallet(account);

    }

    @Override
    public WalletEntity getWalletById(UUID walletId) throws ItemNotFoundException {
        //Make sure only super admin and wallet owner can access this method
        AccountEntity account = getAuthenticatedAccount();
        WalletEntity wallet = initializeWallet(account);

        if (validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), account, wallet)) {
            return wallet;
        } else {
            throw new ItemNotFoundException("You do not have permission to access this wallet");
        }
    }

    @Override
    public WalletEntity getWalletByAccountId(UUID accountId) throws ItemNotFoundException {
        //Make sure only super admin and wallet owner can access this method
        AccountEntity account = accountRepo.findById(accountId)
                .orElseThrow(() -> new ItemNotFoundException("Account not found"));

        WalletEntity wallet = initializeWallet(account);

        if (validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), account, wallet)) {
            return wallet;
        } else {
            throw new ItemNotFoundException("You do not have permission to access this wallet");
        }
    }

    @Override
    public BigDecimal getMyWalletBalance() throws ItemNotFoundException {

        AccountEntity account = getAuthenticatedAccount();
        WalletEntity wallet = initializeWallet(account);

        return calculateBalance(wallet);
    }

    @Override
    public void activateWallet(UUID walletId) throws ItemNotFoundException {
        //Only super admin can activate wallet
        AccountEntity account = getAuthenticatedAccount();
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ItemNotFoundException("Wallet not found"));

        if (!validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN"), account, wallet)) {
            throw new ItemNotFoundException("You do not have permission to activate this wallet");
        }

        wallet.setIsActive(true);
        walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public void deactivateWallet(UUID walletId) throws ItemNotFoundException {

        AccountEntity account = getAuthenticatedAccount();
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ItemNotFoundException("Wallet not found"));

        if (!validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN","ROLE_STAFF_ADMIN"), account, wallet)) {
            throw new ItemNotFoundException("You do not have permission to deactivate this wallet");
        }

        wallet.setIsActive(false);
        walletRepository.save(wallet);
    }

    @Override
    public WalletEntity topupWallet(BigDecimal amount, String description) throws ItemNotFoundException, RandomExceptions {
        //Only super admin and wallet owners can top up wallet
        AccountEntity account = getAuthenticatedAccount();
        WalletEntity wallet = initializeWallet(account);
        if (!validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), account, wallet)) {
            throw new ItemNotFoundException("You do not have permission to topup this wallet");
        }

        //Todo: Integrate with payment gateway to process payment before crediting
        //Todo: Make sure the process is successful before crediting wallet by using webhooks or payment gateway callbacks

        UUID randomOrderId = UUID.randomUUID();
        transactionService.createTransaction(wallet, WALLET_TOPUP, amount, description, randomOrderId);

        return walletRepository.save(wallet);
    }

    @Override
    public WalletEntity withdrawFromWallet(BigDecimal amount, String description) throws ItemNotFoundException, RandomExceptions {
        //Only super admin and wallet owners can withdraw from wallet
        AccountEntity account = getAuthenticatedAccount();
        WalletEntity wallet = initializeWallet(account);

        AccountEntity authAccount = getAuthenticatedAccount();
        if (!validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), authAccount, wallet)) {
            throw new ItemNotFoundException("You do not have permission to withdraw from this wallet");
        }

        BigDecimal currentBalance = calculateBalance(wallet);
        //Todo: Integrate with payment gateway to process withdrawal
        //Todo: Make sure the process is successful before debiting wallet by using webhooks or payment gateway callbacks
        //Todo: Implement withdrawal limits and fees if necessary

        if (currentBalance.compareTo(amount) < 0) {
            throw new RandomExceptions("You have insufficient balance to withdraw the requested amount");
        }

        //Placeholder for order
        UUID randomOrderId = UUID.randomUUID();
        transactionService.createTransaction(wallet, WALLET_WITHDRAWAL, amount, description, randomOrderId);

        return walletRepository.save(wallet);
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
        // Check if the user has any of the custom roles
        boolean hasCustomRole = account.getRoles().stream()
                .anyMatch(role -> customRoles.contains(role.getRoleName()));

        // Check if the user is the owner of the shop
        boolean isOwner = wallet.getAccount().getAccountId().equals(account.getAccountId());

        return hasCustomRole || isOwner;
    }

    public BigDecimal calculateBalance(WalletEntity wallet) {

        List<TransactionEntity> transactions = transactionRepo.findByWallet(wallet);

        BigDecimal balance = BigDecimal.ZERO;
        for (TransactionEntity transaction : transactions) {
            if (isPositiveTransaction(transaction.getTransactionType())) {
                balance = balance.add(transaction.getAmount());      // +
            } else {
                balance = balance.subtract(transaction.getAmount()); // -
            }
        }
        return balance;
    }

    // Positive transactions (money coming in)
    private boolean isPositiveTransaction(TransactionType type) {
        return type == WALLET_TOPUP ||
                type == PURCHASE_REFUND ||
                type == SALE_EARNINGS;
    }
}