package org.nextgate.nextgatebackend.financial_system.payment_processing.callbacks.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.financial_system.payment_processing.callbacks.PaymentCallback;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResult;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.group_purchase_mng.service.GroupPurchaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCallbackImpl implements PaymentCallback {

    private final GroupPurchaseService groupPurchaseService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentSuccess(
            CheckoutSessionEntity checkoutSession,
            EscrowAccountEntity escrow) throws BadRequestException, ItemNotFoundException {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║          PAYMENT SUCCESS CALLBACK                          ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("Checkout Session ID: {}", checkoutSession.getSessionId());
        log.info("Session Type: {}", checkoutSession.getSessionType());
        log.info("Escrow Number: {}", escrow.getEscrowNumber());
        log.info("Total Amount: {} {}", escrow.getTotalAmount(), escrow.getCurrency());
        log.info("Platform Fee: {} {}", escrow.getPlatformFeeAmount(), escrow.getCurrency());
        log.info("Seller Amount: {} {}", escrow.getSellerAmount(), escrow.getCurrency());
        log.info("Buyer: {}", checkoutSession.getCustomer().getUserName());
        log.info("Seller: {}", escrow.getSeller().getUserName());

        //We can have specific actions based on session type
        switch (checkoutSession.getSessionType()){
            case INSTALLMENT -> logPlaceholderAction("Something to be done about installment when successful");
            case REGULAR_DIRECTLY -> logPlaceholderAction("Something to be done about regular directly when successful");
            case REGULAR_CART -> logPlaceholderAction("Something to be done about regular cart when successful");
            case GROUP_PURCHASE -> handleGroupPurchase(checkoutSession);
            default -> log.warn("Unknown session type - no specific actions");

        }

        try {
            // Placeholder actions - will implement step by step
            logPlaceholderAction("Create order from checkout session");
            logPlaceholderAction("Update inventory (deduct sold items)");

            if (checkoutSession.getCartId() != null) {
                logPlaceholderAction("Clear cart for cart-based checkout");
            }

            logPlaceholderAction("Send payment success email to buyer");
            logPlaceholderAction("Send new order notification to seller");
            logPlaceholderAction("Track payment success event for analytics");

            log.info("✓ Payment success callback completed");

        } catch (Exception e) {
            log.error("✗ Error in payment success callback (non-critical)", e);
            // Don't throw - payment already succeeded
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentFailure(
            CheckoutSessionEntity checkoutSession,
            PaymentResult result,
            String errorMessage) {

        log.warn("╔════════════════════════════════════════════════════════════╗");
        log.warn("║          PAYMENT FAILURE CALLBACK                          ║");
        log.warn("╚════════════════════════════════════════════════════════════╝");
        log.warn("Checkout Session ID: {}", checkoutSession.getSessionId());
        log.warn("Error Message: {}", errorMessage);
        log.warn("Payment Status: {}", result.getStatus());
        log.warn("Error Code: {}", result.getErrorCode());
        log.warn("Attempt #: {}", checkoutSession.getPaymentAttemptCount());

        try {
            // Placeholder actions
            if (checkoutSession.getInventoryHeld() != null && checkoutSession.getInventoryHeld()) {
                logPlaceholderAction("Release held inventory");
                for (CheckoutSessionEntity.CheckoutItem item : checkoutSession.getItems()) {
                    log.warn("  → Release {} units of product {} ({})",
                            item.getQuantity(),
                            item.getProductName(),
                            item.getProductId());
                }
            }

            logPlaceholderAction("Send payment failure notification to buyer");
            logPlaceholderAction("Track payment failure event for analytics");

            int attemptCount = checkoutSession.getPaymentAttemptCount();
            if (attemptCount < 5) {
                log.warn("User can retry payment ({}/5 attempts)", attemptCount);
                logPlaceholderAction("Send retry instructions email");
            } else {
                log.warn("Max payment attempts reached - session will expire");
                logPlaceholderAction("Send session expiration notice");
            }

            log.warn("✓ Payment failure callback completed");

        } catch (Exception e) {
            log.error("✗ Error in payment failure callback", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentPending(
            CheckoutSessionEntity checkoutSession,
            PaymentResult result) {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║          PAYMENT PENDING CALLBACK                          ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("Checkout Session ID: {}", checkoutSession.getSessionId());
        log.info("External Reference: {}", result.getExternalReference());
        log.info("Payment URL: {}", result.getPaymentUrl());
        log.info("USSD Code: {}", result.getUssdCode());

        try {
            // Placeholder actions
            logPlaceholderAction("Send payment pending notification with payment instructions");
            logPlaceholderAction("Extend session expiration for external payment");
            logPlaceholderAction("Schedule periodic payment status check");

            log.info("✓ Payment pending callback completed");

        } catch (Exception e) {
            log.error("✗ Error in payment pending callback", e);
        }
    }

    /**
     * Helper method to log placeholder actions consistently
     */
    private void logPlaceholderAction(String action) {
        log.info("  [TODO] {}", action);
    }

    private void handleGroupPurchase(CheckoutSessionEntity checkoutSession) throws BadRequestException, ItemNotFoundException {
        if (checkoutSession.getGroupIdToBeJoined() != null) {
              groupPurchaseService.joinGroup(checkoutSession);
        } else {
           groupPurchaseService.createGroupInstance(checkoutSession);
        }
    }
}