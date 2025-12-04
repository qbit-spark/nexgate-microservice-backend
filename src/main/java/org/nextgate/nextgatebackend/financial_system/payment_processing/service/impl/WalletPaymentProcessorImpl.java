package org.nextgate.nextgatebackend.financial_system.payment_processing.service.impl;


import org.nextgate.nextgatebackend.financial_system.payment_processing.contract.PayableCheckoutSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.service.EscrowService;
import org.nextgate.nextgatebackend.financial_system.payment_processing.enums.PaymentStatus;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResult;
import org.nextgate.nextgatebackend.financial_system.payment_processing.service.WalletPaymentProcessor;
import org.nextgate.nextgatebackend.financial_system.payment_processing.strategy.SessionMetadataExtractorRegistry;
import org.nextgate.nextgatebackend.financial_system.wallet.entity.WalletEntity;
import org.nextgate.nextgatebackend.financial_system.wallet.service.WalletService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletPaymentProcessorImpl implements WalletPaymentProcessor {

    private final WalletService walletService;
    private final EscrowService escrowService;
    private final SessionMetadataExtractorRegistry extractorRegistry;


    @Override
    @Transactional
    public PaymentResult processPayment(PayableCheckoutSession session)
            throws ItemNotFoundException, RandomExceptions {

        try {
            AccountEntity payer = session.getPayer();
            AccountEntity payee = extractorRegistry
                    .getExtractor(session.getSessionDomain())
                    .extractPayee(session);

            BigDecimal totalAmount = session.getTotalAmount();

            log.info("Processing wallet payment | Session: {} | Domain: {} | Amount: {} TZS",
                    session.getSessionId(), session.getSessionDomain(), totalAmount);

            if (session.getEscrowId() != null) {
                log.warn("Escrow already exists for session: {}", session.getSessionId());
                return PaymentResult.builder()
                        .status(PaymentStatus.FAILED)
                        .message("Payment already processed")
                        .errorCode("DUPLICATE_PAYMENT")
                        .build();
            }

            WalletEntity payerWallet = walletService.getWalletByAccountId(payer.getAccountId());

            if (!payerWallet.getIsActive()) {
                throw new RandomExceptions("Wallet is not active");
            }

            BigDecimal walletBalance = walletService.getWalletBalance(payerWallet);

            if (walletBalance.compareTo(totalAmount) < 0) {
                return PaymentResult.builder()
                        .status(PaymentStatus.FAILED)
                        .message(String.format("Insufficient balance. Required: %s, Available: %s",
                                totalAmount, walletBalance))
                        .errorCode("INSUFFICIENT_BALANCE")
                        .build();
            }

            // ✅ NO MORE CASTING! Clean universal call
            EscrowAccountEntity escrow = escrowService.holdMoney(session, payer, payee, totalAmount);

            log.info("✅ Payment successful | Escrow: {}", escrow.getEscrowNumber());

            return PaymentResult.builder()
                    .status(PaymentStatus.SUCCESS)
                    .message("Payment successful")
                    .escrow(escrow)
                    .build();

        } catch (Exception e) {
            log.error("Payment failed: {}", e.getMessage());
            throw e;
        }
    }

}