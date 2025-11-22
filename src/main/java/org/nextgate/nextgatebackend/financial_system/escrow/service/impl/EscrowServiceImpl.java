package org.nextgate.nextgatebackend.financial_system.escrow.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.enums.EscrowStatus;
import org.nextgate.nextgatebackend.financial_system.escrow.repo.EscrowAccountRepo;
import org.nextgate.nextgatebackend.financial_system.escrow.service.EscrowService;
import org.nextgate.nextgatebackend.financial_system.ledger.entity.LedgerAccountEntity;
import org.nextgate.nextgatebackend.financial_system.ledger.entity.LedgerEntryEntity;
import org.nextgate.nextgatebackend.financial_system.ledger.enums.LedgerEntryType;
import org.nextgate.nextgatebackend.financial_system.ledger.service.LedgerService;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionDirection;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionType;
import org.nextgate.nextgatebackend.financial_system.transaction_history.service.TransactionHistoryService;
import org.nextgate.nextgatebackend.financial_system.wallet.entity.WalletEntity;
import org.nextgate.nextgatebackend.financial_system.wallet.service.WalletService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EscrowServiceImpl implements EscrowService {

    private final EscrowAccountRepo escrowAccountRepo;
    private final LedgerService ledgerService;
    private final WalletService walletService;
    private final TransactionHistoryService transactionHistoryService;

    @Value("${app.platform.fee-percentage:5.0}")
    private BigDecimal platformFeePercentage;

    @Override
    @Transactional
    public EscrowAccountEntity holdMoney(CheckoutSessionEntity checkoutSession, AccountEntity buyer, AccountEntity seller, BigDecimal amount) throws ItemNotFoundException, RandomExceptions {

        // Prevent duplicate escrow for same checkout session
        if (escrowExistsForCheckoutSession(checkoutSession)) {
            throw new RandomExceptions("Escrow already exists for this checkout session");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RandomExceptions("Escrow amount must be greater than zero");
        }

        // Generate unique escrow number (e.g., ESC-2025-000001)
        String escrowNumber = generateEscrowNumber();

        // ========================================
        // STEP 1: Create and save escrow entity FIRST
        // ========================================
        EscrowAccountEntity escrow = EscrowAccountEntity.builder()
                .escrowNumber(escrowNumber)
                .checkoutSession(checkoutSession)
                .buyer(buyer)
                .seller(seller)
                .totalAmount(amount)
                .platformFeePercentage(platformFeePercentage)
                .status(EscrowStatus.HELD)
                .build();

        // Calculate platform fee and seller amount (e.g., 5% fee, 95% to seller)
        escrow.calculateFees();

        // Save escrow to get the ID
        EscrowAccountEntity savedEscrow = escrowAccountRepo.save(escrow);

        log.info("Escrow entity created with ID: {}", savedEscrow.getId());

        // ========================================
        // STEP 2: Now create ledger account using escrow ID
        // ========================================
        LedgerAccountEntity escrowLedgerAccount = ledgerService.createEscrowAccount(savedEscrow.getId());

        // Update escrow with ledger account ID
        savedEscrow.setLedgerAccountId(escrowLedgerAccount.getId());

        // ========================================
        // STEP 3: Get buyer's wallet ledger account
        // ========================================
        WalletEntity buyerWallet = walletService.getWalletByAccountId(buyer.getAccountId());
        LedgerAccountEntity buyerLedgerAccount = ledgerService.getOrCreateWalletAccount(buyerWallet);

        // ========================================
        // STEP 4: Move money from buyer wallet to escrow
        // ========================================
        LedgerEntryEntity ledgerEntry = ledgerService.createEntry(
                buyerLedgerAccount,
                escrowLedgerAccount,
                amount,
                LedgerEntryType.PURCHASE,
                "CHECKOUT_SESSION",
                checkoutSession.getSessionId(),
                String.format("Purchase escrow for checkout session %s", checkoutSession.getSessionId()),
                buyer
        );

        // ========================================
        // STEP 5: Save escrow again with ledger account ID
        // ========================================
        savedEscrow = escrowAccountRepo.save(savedEscrow);

        // ========================================
        // STEP 6: Create transaction history
        // ========================================
        // Create transaction history for BUYER (PURCHASE)
        transactionHistoryService.createTransaction(
                buyer,
                TransactionType.PURCHASE,
                TransactionDirection.DEBIT,
                amount,
                "Purchase Payment",
                String.format("Payment for order (Escrow: %s)", savedEscrow.getEscrowNumber()),
                ledgerEntry.getId(),
                "ESCROW",
                savedEscrow.getId()
        );

        // Create transaction history for ADMIN tracking (ESCROW_HOLD)
        transactionHistoryService.createTransaction(
                buyer,
                TransactionType.ESCROW_HOLD,
                TransactionDirection.DEBIT,
                amount,
                "Escrow Hold",
                String.format("Money held in escrow: %s", savedEscrow.getEscrowNumber()),
                ledgerEntry.getId(),
                "ESCROW",
                savedEscrow.getId()
        );

        log.info("Escrow created: {} - Amount: {} TZS, Fee: {} TZS, Seller receives: {} TZS",
                escrowNumber, amount, savedEscrow.getPlatformFeeAmount(), savedEscrow.getSellerAmount());

        return savedEscrow;
    }

    @Override
    @Transactional
    public void releaseMoney(UUID escrowId) throws ItemNotFoundException, RandomExceptions {

        EscrowAccountEntity escrow = getEscrowById(escrowId);

        // Only HELD escrows can be released
        if (!escrow.canRelease()) {
            throw new RandomExceptions("Cannot release escrow - current status: " + escrow.getStatus());
        }

        // Get escrow ledger account (source of money)
        LedgerAccountEntity escrowLedgerAccount = ledgerService.getAccountById(escrow.getLedgerAccountId());

        // Get seller's wallet ledger account (destination 1)
        WalletEntity sellerWallet = walletService.getWalletByAccountIdInternalUse(escrow.getSeller().getAccountId());
        LedgerAccountEntity sellerLedgerAccount = ledgerService.getOrCreateWalletAccount(sellerWallet);

        // Get platform revenue account (destination 2)
        LedgerAccountEntity platformRevenueAccount = ledgerService.getPlatformRevenueAccount();

        // Split escrow money: seller gets 95%, platform gets 5%
        Map<LedgerAccountEntity, BigDecimal> creditAccounts = new HashMap<>();
        creditAccounts.put(sellerLedgerAccount, escrow.getSellerAmount());
        creditAccounts.put(platformRevenueAccount, escrow.getPlatformFeeAmount());


        // Execute split: one debit (escrow), two credits (seller + platform)
        List<LedgerEntryEntity> ledgerEntries = ledgerService.createSplitEntry(
                escrowLedgerAccount,
                creditAccounts,
                LedgerEntryType.ESCROW_RELEASE,
                "ESCROW",
                escrowId,
                String.format("Escrow release for %s", escrow.getEscrowNumber()),
                null
        );

        // Update escrow status and timestamp
        escrow.markAsReleased();
        escrowAccountRepo.save(escrow);

        // Create transaction history for SELLER (SALE)
        transactionHistoryService.createTransaction(
                escrow.getSeller(),
                TransactionType.SALE,
                TransactionDirection.CREDIT,
                escrow.getSellerAmount(),
                "Sale Earnings",
                String.format("Payment received for sale (Escrow: %s)", escrow.getEscrowNumber()),
                ledgerEntries.get(0).getId(),
                "ESCROW",
                escrowId
        );

        // Create transaction history for PLATFORM (PLATFORM_FEE_COLLECTED)
        // Using buyer account temporarily - TODO: Replace with platform admin account
        transactionHistoryService.createTransaction(
                escrow.getBuyer(),
                TransactionType.PLATFORM_FEE_COLLECTED,
                TransactionDirection.CREDIT,
                escrow.getPlatformFeeAmount(),
                "Platform Fee Collected",
                String.format("Fee collected from escrow: %s", escrow.getEscrowNumber()),
                ledgerEntries.get(1).getId(),
                "ESCROW",
                escrowId
        );

        log.info("Escrow released: {} - Seller received: {} TZS, Platform fee: {} TZS",
                escrow.getEscrowNumber(), escrow.getSellerAmount(), escrow.getPlatformFeeAmount());

        //Todo: We need to send a notification to the seller

    }

    @Override
    @Transactional
    public void refundMoney(UUID escrowId) throws ItemNotFoundException, RandomExceptions {

        EscrowAccountEntity escrow = getEscrowById(escrowId);

        // Only HELD or DISPUTED escrows can be refunded
        if (!escrow.canRefund()) {
            throw new RandomExceptions("Cannot refund escrow - current status: " + escrow.getStatus());
        }

        // Get escrow ledger account (source)
        LedgerAccountEntity escrowLedgerAccount = ledgerService.getAccountById(escrow.getLedgerAccountId());

        // Get buyer's wallet ledger account (destination)
        WalletEntity buyerWallet = walletService.getWalletByAccountId(escrow.getBuyer().getAccountId());
        LedgerAccountEntity buyerLedgerAccount = ledgerService.getOrCreateWalletAccount(buyerWallet);

        // Return full amount to buyer (no fees deducted)
        LedgerEntryEntity ledgerEntry = ledgerService.createEntry(
                escrowLedgerAccount,
                buyerLedgerAccount,
                escrow.getTotalAmount(),
                LedgerEntryType.REFUND,
                "ESCROW",
                escrowId,
                String.format("Escrow refund for %s", escrow.getEscrowNumber()),
                null
        );

        // Update escrow status and timestamp
        escrow.markAsRefunded();
        escrowAccountRepo.save(escrow);

        // Create transaction history for BUYER (PURCHASE_REFUND)
        transactionHistoryService.createTransaction(
                escrow.getBuyer(),
                TransactionType.PURCHASE_REFUND,
                TransactionDirection.CREDIT,
                escrow.getTotalAmount(),
                "Refund Received",
                String.format("Refund for cancelled order (Escrow: %s)", escrow.getEscrowNumber()),
                ledgerEntry.getId(),
                "ESCROW",
                escrowId
        );

        // Create transaction history for SELLER (SALE_REFUND) - tracking only
        transactionHistoryService.createTransaction(
                escrow.getSeller(),
                TransactionType.SALE_REFUND,
                TransactionDirection.DEBIT,
                escrow.getSellerAmount(),
                "Sale Refund Issued",
                String.format("Refund issued for order (Escrow: %s)", escrow.getEscrowNumber()),
                ledgerEntry.getId(),
                "ESCROW",
                escrowId
        );

        log.info("Escrow refunded: {} - Amount: {} TZS returned to buyer",
                escrow.getEscrowNumber(), escrow.getTotalAmount());

        //Todo: We need to send a notification to the buyer about the refund
    }

    @Override
    @Transactional
    public void disputeEscrow(UUID escrowId) throws ItemNotFoundException, RandomExceptions {

        EscrowAccountEntity escrow = getEscrowById(escrowId);

        // Can only dispute HELD escrows
        if (!escrow.isHeld()) {
            throw new RandomExceptions("Cannot dispute escrow - current status: " + escrow.getStatus());
        }

        // Mark as disputed but money stays in escrow (no ledger movement)
        escrow.markAsDisputed();
        escrowAccountRepo.save(escrow);

        log.info("Escrow disputed: {} - Money remains held", escrow.getEscrowNumber());
    }

    @Override
    public EscrowAccountEntity getEscrowById(UUID escrowId) throws ItemNotFoundException {
        return escrowAccountRepo.findById(escrowId)
                .orElseThrow(() -> new ItemNotFoundException("Escrow not found with ID: " + escrowId));
    }

    @Override
    public EscrowAccountEntity getEscrowByNumber(String escrowNumber) throws ItemNotFoundException {
        return escrowAccountRepo.findByEscrowNumber(escrowNumber)
                .orElseThrow(() -> new ItemNotFoundException("Escrow not found: " + escrowNumber));
    }

    @Override
    public EscrowAccountEntity getEscrowByCheckoutSession(CheckoutSessionEntity checkoutSession)
            throws ItemNotFoundException {
        return escrowAccountRepo.findByCheckoutSession(checkoutSession)
                .orElseThrow(() -> new ItemNotFoundException("Escrow not found for checkout session"));
    }

    @Override
    public List<EscrowAccountEntity> getBuyerEscrows(AccountEntity buyer) {
        return escrowAccountRepo.findByBuyerOrderByCreatedAtDesc(buyer);
    }

    @Override
    public List<EscrowAccountEntity> getSellerEscrows(AccountEntity seller) {
        return escrowAccountRepo.findBySellerOrderByCreatedAtDesc(seller);
    }

    @Override
    public List<EscrowAccountEntity> getEscrowsByStatus(EscrowStatus status) {
        return escrowAccountRepo.findByStatusOrderByCreatedAtDesc(status);
    }

    @Override
    public boolean escrowExistsForCheckoutSession(CheckoutSessionEntity checkoutSession) {
        return escrowAccountRepo.existsByCheckoutSession(checkoutSession);
    }

    @Override
    public String generateEscrowNumber() {
        int year = Year.now().getValue();
        long count = escrowAccountRepo.count() + 1;
        String escrowNumber = String.format("ESC-%d-%06d", year, count);

        // Ensure uniqueness by incrementing if exists
        while (escrowAccountRepo.existsByEscrowNumber(escrowNumber)) {
            count++;
            escrowNumber = String.format("ESC-%d-%06d", year, count);
        }

        return escrowNumber;
    }
}