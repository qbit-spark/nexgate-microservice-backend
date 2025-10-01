package org.nextgate.nextgatebackend.financial_system.escrow.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.enums.EscrowStatus;
import org.nextgate.nextgatebackend.financial_system.escrow.repo.EscrowAccountRepo;
import org.nextgate.nextgatebackend.financial_system.escrow.service.EscrowService;
import org.nextgate.nextgatebackend.financial_system.ledger.entity.LedgerAccountEntity;
import org.nextgate.nextgatebackend.financial_system.ledger.enums.LedgerEntryType;
import org.nextgate.nextgatebackend.financial_system.ledger.service.LedgerService;
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

    @Value("${app.platform.fee-percentage:5.0}")
    private BigDecimal platformFeePercentage;

    @Override
    @Transactional
    public EscrowAccountEntity holdMoney(
            CheckoutSessionEntity checkoutSession,
            AccountEntity buyer,
            AccountEntity seller,
            BigDecimal amount) throws ItemNotFoundException, RandomExceptions {

        // Prevent duplicate escrow for same checkout session
        if (escrowExistsForCheckoutSession(checkoutSession)) {
            throw new RandomExceptions("Escrow already exists for this checkout session");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RandomExceptions("Escrow amount must be greater than zero");
        }

        // Generate unique escrow number (e.g., ESC-2025-000001)
        String escrowNumber = generateEscrowNumber();

        // Create escrow entity with buyer, seller, and amount details
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

        // Create ledger account for this escrow (type: ESCROW)
        LedgerAccountEntity escrowLedgerAccount = ledgerService.createEscrowAccount(escrow.getId());
        escrow.setLedgerAccountId(escrowLedgerAccount.getId());

        // Get buyer's wallet ledger account
        WalletEntity buyerWallet = walletService.getWalletByAccountId(buyer.getAccountId());
        LedgerAccountEntity buyerLedgerAccount = ledgerService.getOrCreateWalletAccount(buyerWallet);

        // Move money from buyer wallet to escrow (debit buyer, credit escrow)
        ledgerService.createEntry(
                buyerLedgerAccount,
                escrowLedgerAccount,
                amount,
                LedgerEntryType.PURCHASE,
                "CHECKOUT_SESSION",
                checkoutSession.getSessionId(),
                String.format("Purchase escrow for checkout session %s", checkoutSession.getSessionId()),
                buyer
        );

        EscrowAccountEntity savedEscrow = escrowAccountRepo.save(escrow);

        log.info("Escrow created: {} - Amount: {} TZS, Fee: {} TZS, Seller receives: {} TZS",
                escrowNumber, amount, escrow.getPlatformFeeAmount(), escrow.getSellerAmount());

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
        WalletEntity sellerWallet = walletService.getWalletByAccountId(escrow.getSeller().getAccountId());
        LedgerAccountEntity sellerLedgerAccount = ledgerService.getOrCreateWalletAccount(sellerWallet);

        // Get platform revenue account (destination 2)
        LedgerAccountEntity platformRevenueAccount = ledgerService.getPlatformRevenueAccount();

        // Split escrow money: seller gets 95%, platform gets 5%
        Map<LedgerAccountEntity, BigDecimal> creditAccounts = new HashMap<>();
        creditAccounts.put(sellerLedgerAccount, escrow.getSellerAmount());
        creditAccounts.put(platformRevenueAccount, escrow.getPlatformFeeAmount());

        // Execute split: one debit (escrow), two credits (seller + platform)
        ledgerService.createSplitEntry(
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

        log.info("Escrow released: {} - Seller received: {} TZS, Platform fee: {} TZS",
                escrow.getEscrowNumber(), escrow.getSellerAmount(), escrow.getPlatformFeeAmount());
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
        ledgerService.createEntry(
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

        log.info("Escrow refunded: {} - Amount: {} TZS returned to buyer",
                escrow.getEscrowNumber(), escrow.getTotalAmount());
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