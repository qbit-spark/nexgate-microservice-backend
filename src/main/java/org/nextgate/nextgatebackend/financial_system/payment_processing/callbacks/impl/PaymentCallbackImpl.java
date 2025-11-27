package org.nextgate.nextgatebackend.financial_system.payment_processing.callbacks.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import com.qbitspark.jikoexpress.financial_system.payment_processing.contract.PayableCheckoutSession;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.financial_system.payment_processing.callbacks.PaymentCallback;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResult;
import org.nextgate.nextgatebackend.financial_system.payment_processing.strategy.PostPaymentHandlerRegistry;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCallbackImpl implements PaymentCallback {

    private final PostPaymentHandlerRegistry postPaymentHandlerRegistry;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentSuccess(
            PayableCheckoutSession session,
            EscrowAccountEntity escrow) throws BadRequestException, ItemNotFoundException, RandomExceptions {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║          PAYMENT SUCCESS CALLBACK                          ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("Session: {} | Domain: {} | Escrow: {}",
                session.getSessionId(), session.getSessionDomain(), escrow.getEscrowNumber());
        log.info("Amount: {} {} | Platform Fee: {} | Seller: {}",
                escrow.getTotalAmount(), escrow.getCurrency(),
                escrow.getPlatformFeeAmount(), escrow.getSellerAmount());

        // Route to a domain-specific handler using a strategy pattern
        postPaymentHandlerRegistry
                .getHandler(session.getSessionDomain())
                .handlePostPayment(session, escrow);

        log.info("✓ Payment success callback completed");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentFailure(
            PayableCheckoutSession session,
            PaymentResult result,
            String errorMessage) {

        log.warn("╔════════════════════════════════════════════════════════════╗");
        log.warn("║          PAYMENT FAILURE CALLBACK                          ║");
        log.warn("╚════════════════════════════════════════════════════════════╝");
        log.warn("Session: {} | Domain: {}", session.getSessionId(), session.getSessionDomain());
        log.warn("Error: {}", errorMessage);
        log.warn("Status: {} | Code: {}", result.getStatus(), result.getErrorCode());
        log.warn("Attempt #: {}", session.getPaymentAttemptCount());

        log.warn("[TODO] Release held inventory/tickets");
        log.warn("[TODO] Send failure notification to customer");

        int attemptCount = session.getPaymentAttemptCount();
        if (attemptCount < 5) {
            log.warn("User can retry payment ({}/5 attempts)", attemptCount);
        } else {
            log.warn("Max payment attempts reached - session will expire");
        }

        log.warn("✓ Payment failure callback completed");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentPending(
            PayableCheckoutSession session,
            PaymentResult result) {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║          PAYMENT PENDING CALLBACK                          ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("Session: {} | Domain: {}", session.getSessionId(), session.getSessionDomain());
        log.info("External Reference: {}", result.getExternalReference());
        log.info("Payment URL: {}", result.getPaymentUrl());

        log.info("[TODO] Send payment pending notification");
        log.info("[TODO] Extend session expiration");
        log.info("[TODO] Schedule status check");

        log.info("✓ Payment pending callback completed");
    }
}