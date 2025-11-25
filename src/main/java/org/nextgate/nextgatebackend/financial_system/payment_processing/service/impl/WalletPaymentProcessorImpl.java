package org.nextgate.nextgatebackend.financial_system.payment_processing.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.service.EscrowService;
import org.nextgate.nextgatebackend.financial_system.payment_processing.enums.PaymentStatus;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResult;
import org.nextgate.nextgatebackend.financial_system.payment_processing.service.WalletPaymentProcessor;
import org.nextgate.nextgatebackend.financial_system.wallet.entity.WalletEntity;
import org.nextgate.nextgatebackend.financial_system.wallet.service.WalletService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.repo.ShopRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletPaymentProcessorImpl implements WalletPaymentProcessor {

    private final WalletService walletService;
    private final EscrowService escrowService;
    private final ShopRepo shopRepo;

    @Override
    @Transactional
    public PaymentResult processPayment(CheckoutSessionEntity checkoutSession)
            throws ItemNotFoundException, RandomExceptions {

        try {
            // Extract buyer and seller from checkout session
            var buyer = checkoutSession.getCustomer();
            var seller = extractSellerFromCheckoutSession(checkoutSession);
            var totalAmount = checkoutSession.getPricing().getTotal();

            log.info("Processing wallet payment for checkout session: {}, amount: {} TZS",
                    checkoutSession.getSessionId(), totalAmount);

            // Check if escrow already exists (idempotency)
            if (escrowService.escrowExistsForCheckoutSession(checkoutSession)) {
                log.warn("Escrow already exists for checkout session: {}", checkoutSession.getSessionId());
                return PaymentResult.builder()
                        .status(PaymentStatus.FAILED)
                        .message("Payment already processed for this checkout session")
                        .errorCode("DUPLICATE_PAYMENT")
                        .errorMessage("Escrow already exists")
                        .build();
            }

            // Get buyer's wallet and validate balance
            WalletEntity buyerWallet = walletService.getWalletByAccountId(buyer.getAccountId());

            if (!buyerWallet.getIsActive()) {
                throw new RandomExceptions("Wallet is not active. Please contact support.");
            }

            BigDecimal walletBalance = walletService.getWalletBalance(buyerWallet);

            if (walletBalance.compareTo(totalAmount) < 0) {
                log.warn("Insufficient balance for user: {} - Required: {}, Available: {}",
                        buyer.getUserName(), totalAmount, walletBalance);

                return PaymentResult.builder()
                        .status(PaymentStatus.FAILED)
                        .message(String.format("Insufficient balance. Required: %s TZS, Available: %s TZS",
                                totalAmount, walletBalance))
                        .errorCode("INSUFFICIENT_BALANCE")
                        .errorMessage("Wallet balance too low")
                        .build();
            }

            // Create escrow (this moves money from wallet to escrow)
            EscrowAccountEntity escrow = escrowService.holdMoney(
                    checkoutSession,
                    buyer,
                    seller,
                    totalAmount
            );

            log.info("Wallet payment successful - Escrow created: {}", escrow.getEscrowNumber());

            return PaymentResult.builder()
                    .status(PaymentStatus.SUCCESS)
                    .message("Payment successful")
                    .escrow(escrow)
                    .build();

        } catch (ItemNotFoundException | RandomExceptions e) {
            log.error("Wallet payment failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during wallet payment", e);
            return PaymentResult.builder()
                    .status(PaymentStatus.FAILED)
                    .message("Payment processing failed")
                    .errorCode("PROCESSING_ERROR")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    // Extracts seller from checkout session items
    private AccountEntity extractSellerFromCheckoutSession(
            CheckoutSessionEntity checkoutSession) throws RandomExceptions {

        // Get first item's shop owner as seller
        if (checkoutSession.getItems() == null || checkoutSession.getItems().isEmpty()) {
            throw new RandomExceptions("No items found in checkout session");
        }

        var firstItem = checkoutSession.getItems().getFirst();
        var shopId = firstItem.getShopId();

        var shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new RandomExceptions("Shop not found for ID: " + shopId));

        // TODO: Fetch actual seller from shop service
        return shop.getOwner();
    }
}