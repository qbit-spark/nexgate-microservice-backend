package org.nextgate.nextgatebackend.financial_system.escrow.service.impl;

import com.qbitspark.jikoexpress.financial_system.payment_processing.contract.PayableCheckoutSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
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
import org.nextgate.nextgatebackend.globe_enums.CheckoutSessionsDomains;
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
    public EscrowAccountEntity holdMoney(
            PayableCheckoutSession session,
            AccountEntity payer,
            AccountEntity payee,
            BigDecimal amount) throws ItemNotFoundException, RandomExceptions {

        // Prevent duplicate escrow for same session
        if (escrowExistsForSession(session.getSessionId(), session.getSessionDomain())) {
            throw new RandomExceptions("Escrow already exists for this checkout session");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RandomExceptions("Escrow amount must be greater than zero");
        }

        String escrowNumber = generateEscrowNumber();

        // Create escrow entity
        EscrowAccountEntity escrow = EscrowAccountEntity.builder()
                .escrowNumber(escrowNumber)
                .checkoutSessionId(session.getSessionId())
                .sessionDomain(session.getSessionDomain())
                .buyer(payer)
                .seller(payee)
                .totalAmount(amount)
                .platformFeePercentage(platformFeePercentage)
                .status(EscrowStatus.HELD)
                .currency(session.getCurrency())
                .build();

        escrow.calculateFees();

        EscrowAccountEntity savedEscrow = escrowAccountRepo.save(escrow);

        log.info("Escrow entity created | ID: {} | Domain: {}", savedEscrow.getId(), session.getSessionDomain());

        // Create ledger account for escrow
        LedgerAccountEntity escrowLedgerAccount = ledgerService.createEscrowAccount(savedEscrow.getId());
        savedEscrow.setLedgerAccountId(escrowLedgerAccount.getId());

        // Get payer's wallet ledger account
        WalletEntity payerWallet = walletService.getWalletByAccountId(payer.getAccountId());
        LedgerAccountEntity payerLedgerAccount = ledgerService.getOrCreateWalletAccount(payerWallet);

        // Move money from payer wallet to escrow
        LedgerEntryEntity ledgerEntry = ledgerService.createEntry(
                payerLedgerAccount,
                escrowLedgerAccount,
                amount,
                LedgerEntryType.PURCHASE,
                session.getSessionDomain() + "_CHECKOUT",
                session.getSessionId(),
                String.format("%s purchase escrow for session %s", session.getSessionDomain(), session.getSessionId()),
                payer
        );

        savedEscrow = escrowAccountRepo.save(savedEscrow);

        // Create a transaction history for PAYER
        transactionHistoryService.createTransaction(
                payer,
                TransactionType.PURCHASE,
                TransactionDirection.DEBIT,
                amount,
                "Purchase Payment",
                String.format("Payment for %s (Escrow: %s)", session.getSessionDomain().name().toLowerCase(), savedEscrow.getEscrowNumber()),
                ledgerEntry.getId(),
                "ESCROW",
                savedEscrow.getId()
        );

        // Create transaction history for ADMIN tracking
        transactionHistoryService.createTransaction(
                payer,
                TransactionType.ESCROW_HOLD,
                TransactionDirection.DEBIT,
                amount,
                "Escrow Hold",
                String.format("Money held in escrow: %s", savedEscrow.getEscrowNumber()),
                ledgerEntry.getId(),
                "ESCROW",
                savedEscrow.getId()
        );

        log.info("✅ Escrow created: {} | Domain: {} | Amount: {} | Fee: {} | Seller: {}",
                escrowNumber, session.getSessionDomain(), amount, savedEscrow.getPlatformFeeAmount(), savedEscrow.getSellerAmount());

        return savedEscrow;
    }

    @Override
    @Transactional
    public void releaseMoney(UUID escrowId) throws ItemNotFoundException, RandomExceptions {

        EscrowAccountEntity escrow = getEscrowById(escrowId);

        if (!escrow.canRelease()) {
            throw new RandomExceptions("Cannot release escrow - current status: " + escrow.getStatus());
        }

        LedgerAccountEntity escrowLedgerAccount = ledgerService.getAccountById(escrow.getLedgerAccountId());

        WalletEntity sellerWallet = walletService.getWalletByAccountIdInternalUse(escrow.getSeller().getAccountId());
        LedgerAccountEntity sellerLedgerAccount = ledgerService.getOrCreateWalletAccount(sellerWallet);

        LedgerAccountEntity platformRevenueAccount = ledgerService.getPlatformRevenueAccount();

        Map<LedgerAccountEntity, BigDecimal> creditAccounts = new HashMap<>();
        creditAccounts.put(sellerLedgerAccount, escrow.getSellerAmount());
        creditAccounts.put(platformRevenueAccount, escrow.getPlatformFeeAmount());

        List<LedgerEntryEntity> ledgerEntries = ledgerService.createSplitEntry(
                escrowLedgerAccount,
                creditAccounts,
                LedgerEntryType.ESCROW_RELEASE,
                "ESCROW",
                escrowId,
                String.format("Escrow release for %s", escrow.getEscrowNumber()),
                null
        );

        escrow.markAsReleased();
        escrowAccountRepo.save(escrow);

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

        log.info("✅ Escrow released: {} | Seller: {} | Platform: {}",
                escrow.getEscrowNumber(), escrow.getSellerAmount(), escrow.getPlatformFeeAmount());
    }

    @Override
    @Transactional
    public void refundMoney(UUID escrowId) throws ItemNotFoundException, RandomExceptions {

        EscrowAccountEntity escrow = getEscrowById(escrowId);

        if (!escrow.canRefund()) {
            throw new RandomExceptions("Cannot refund escrow - current status: " + escrow.getStatus());
        }

        LedgerAccountEntity escrowLedgerAccount = ledgerService.getAccountById(escrow.getLedgerAccountId());

        WalletEntity buyerWallet = walletService.getWalletByAccountId(escrow.getBuyer().getAccountId());
        LedgerAccountEntity buyerLedgerAccount = ledgerService.getOrCreateWalletAccount(buyerWallet);

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

        escrow.markAsRefunded();
        escrowAccountRepo.save(escrow);

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

        log.info("✅ Escrow refunded: {} | Amount: {} returned to buyer",
                escrow.getEscrowNumber(), escrow.getTotalAmount());
    }

    @Override
    @Transactional
    public void disputeEscrow(UUID escrowId) throws ItemNotFoundException, RandomExceptions {

        EscrowAccountEntity escrow = getEscrowById(escrowId);

        if (!escrow.isHeld()) {
            throw new RandomExceptions("Cannot dispute escrow - current status: " + escrow.getStatus());
        }

        escrow.markAsDisputed();
        escrowAccountRepo.save(escrow);

        log.info("Escrow disputed: {} - Money remains held", escrow.getEscrowNumber());
    }

    @Override
    public EscrowAccountEntity getEscrowById(UUID escrowId) throws ItemNotFoundException {
        return escrowAccountRepo.findById(escrowId)
                .orElseThrow(() -> new ItemNotFoundException("Escrow not found: " + escrowId));
    }

    @Override
    public EscrowAccountEntity getEscrowByNumber(String escrowNumber) throws ItemNotFoundException {
        return escrowAccountRepo.findByEscrowNumber(escrowNumber)
                .orElseThrow(() -> new ItemNotFoundException("Escrow not found: " + escrowNumber));
    }

    @Override
    public EscrowAccountEntity getEscrowBySessionId(UUID sessionId, CheckoutSessionsDomains sessionDomain) throws ItemNotFoundException {
        return escrowAccountRepo.findByCheckoutSessionIdAndSessionDomain(sessionId, sessionDomain)
                .orElseThrow(() -> new ItemNotFoundException("Escrow not found for session"));
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
    public boolean escrowExistsForSession(UUID sessionId, CheckoutSessionsDomains sessionDomain) {
        return escrowAccountRepo.existsByCheckoutSessionIdAndSessionDomain(sessionId, sessionDomain);
    }

    @Override
    public String generateEscrowNumber() {
        int year = Year.now().getValue();
        long count = escrowAccountRepo.count() + 1;
        String escrowNumber = String.format("ESC-%d-%06d", year, count);

        while (escrowAccountRepo.existsByEscrowNumber(escrowNumber)) {
            count++;
            escrowNumber = String.format("ESC-%d-%06d", year, count);
        }

        return escrowNumber;
    }
}