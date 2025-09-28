package org.nextgate.nextgatebackend.wallet_service.transactions.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepo transactionRepository;
    private final AccountRepo accountRepo;

    private static final String TRANSACTION_REF_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    @Override
    public TransactionEntity createTransaction(WalletEntity wallet, TransactionType transactionType, BigDecimal amount, String description, UUID orderId) {

        // Create transaction
        TransactionEntity transaction = TransactionEntity.builder()
                .account(wallet.getAccount())
                .wallet(wallet)
                .transactionRef(generateTransactionRef())
                .transactionType(transactionType)
                .amount(amount)
                .status(TransactionStatus.PENDING)
                .description(description)
                .orderId(orderId)
                .createdAt(LocalDateTime.now())
                .build();

        // Save transaction
        TransactionEntity savedTransaction = transactionRepository.save(transaction);

        // Send notification (placeholder)
        sendNotificationPlaceholder(savedTransaction);

        return savedTransaction;
    }

    @Override
    public TransactionEntity updateTransactionStatus(UUID transactionId, TransactionStatus status) throws ItemNotFoundException {
        TransactionEntity transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ItemNotFoundException("Transaction not found with ID: " + transactionId));
        transaction.setStatus(status);
        return transactionRepository.save(transaction);
    }

    //Todo: This can be accessible form controllers
    //Make sure only super admin, staff admin and wallet owner can access this method
    @Override
    public TransactionEntity getTransactionById(UUID transactionId) throws ItemNotFoundException {
        AccountEntity account = getAuthenticatedAccount();
        TransactionEntity transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ItemNotFoundException("Transaction not found"));

        if (validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), account, transaction)) {
            return transaction;
        } else {
            throw new ItemNotFoundException("You do not have permission to access this transaction");
        }

    }

    //Todo: Ony admin and wallet owner can access this method
    @Override
    public List<TransactionEntity> getUserTransactions(UUID accountId) throws ItemNotFoundException {

        AccountEntity account = accountRepo.findById(accountId)
                .orElseThrow(() -> new ItemNotFoundException("Account not found"));

        if (validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), account)) {
            return transactionRepository.findByAccount(account);
        } else {
            throw new ItemNotFoundException("You do not have permission to access these transactions");
        }
    }

    @Override
    public Page<TransactionEntity> getUserTransactionsPaged(UUID accountId, int page, int size) throws ItemNotFoundException {
        AccountEntity account = accountRepo.findById(accountId)
                .orElseThrow(() -> new ItemNotFoundException("Account not found"));

        if (validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), account)) {
            return transactionRepository.findByAccountOrderByCreatedAtDesc(account, PageRequest.of(page, size));
        } else {
            throw new ItemNotFoundException("You do not have permission to access these transactions");
        }
    }

    @Override
    public TransactionEntity createPurchaseTransaction(AccountEntity buyer, AccountEntity seller, BigDecimal amount, String orderId, String description, boolean useWallet) {
        //Todo: Theirs a lot here
        //Todo: Make sure buyer has enough balance if useWallet is true
        //Todo: If useWallet is false, create a pending transaction and wait for payment gateway confirmation
        //Todo: Deduct from buyer wallet and add to escrow wallet(Create these transactions)

        return null;
    }

    //Todo: This can be work for controller
    @Override
    public List<TransactionEntity> getMyTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) throws ItemNotFoundException {
        AccountEntity account = getAuthenticatedAccount();

        return transactionRepository.findByAccountAndCreatedAtBetweenOrderByCreatedAtDesc(account, startDate, endDate);
    }

    //Todo: This can be work for controller
    @Override
    public Page<TransactionEntity> getMyTransactionsByDateRangePaged(LocalDateTime startDate, LocalDateTime endDate, int page, int size) throws ItemNotFoundException {
        AccountEntity account = getAuthenticatedAccount();

        return transactionRepository.findByAccountAndCreatedAtBetweenOrderByCreatedAtDesc(account, startDate, endDate, PageRequest.of(page, size));
    }


    public String generateTransactionRef() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        StringBuilder ref = new StringBuilder("#" + year);

        // Generate 4 random characters
        for (int i = 0; i < 4; i++) {
            ref.append(TRANSACTION_REF_CHARS.charAt(RANDOM.nextInt(TRANSACTION_REF_CHARS.length())));
        }

        // Ensure uniqueness
        while (transactionRepository.existsByTransactionRef(ref.toString())) {
            ref = new StringBuilder("#" + year);
            for (int i = 0; i < 4; i++) {
                ref.append(TRANSACTION_REF_CHARS.charAt(RANDOM.nextInt(TRANSACTION_REF_CHARS.length())));
            }
        }

        return ref.toString();
    }

    private void sendNotificationPlaceholder(TransactionEntity transaction) {
        log.info("NOTIFICATION PLACEHOLDER: Transaction {} - {} {} for account {}",
                transaction.getTransactionRef(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getAccount().getId());
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

    private boolean validateSystemRolesOrOwner(List<String> customRoles, AccountEntity account, TransactionEntity transaction) {
        // Check if the user has any of the custom roles
        boolean hasCustomRole = account.getRoles().stream()
                .anyMatch(role -> customRoles.contains(role.getRoleName()));

        // Check if the user is the owner of the shop
        boolean isOwner = transaction.getAccount().getAccountId().equals(account.getAccountId());

        return hasCustomRole || isOwner;
    }

    private boolean validateSystemRolesOrOwner(List<String> customRoles, AccountEntity account) {
        // Check if the user has any of the custom roles

        return account.getRoles().stream()
                .anyMatch(role -> customRoles.contains(role.getRoleName()));
    }


}