package org.nextgate.nextgatebackend.financial_system.payment_processing.callbacks.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.checkout_session.repo.CheckoutSessionRepo;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.financial_system.payment_processing.callbacks.PaymentCallback;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResult;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.group_purchase_mng.service.GroupPurchaseService;
import org.nextgate.nextgatebackend.installment_purchase.service.InstallmentService; // ADD THIS
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCallbackImpl implements PaymentCallback {

    private final GroupPurchaseService groupPurchaseService;
    private final InstallmentService installmentService;
    private final CheckoutSessionRepo checkoutSessionRepo;

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
        log.info("Status: {}", checkoutSession.getStatus());

        // Route to specific handler based on session type
        switch (checkoutSession.getSessionType()) {
            case INSTALLMENT -> handleInstallmentPayment(checkoutSession, escrow);
            case REGULAR_DIRECTLY -> handleRegularDirectly(checkoutSession, escrow);
            case REGULAR_CART -> handleRegularCart(checkoutSession, escrow);
            case GROUP_PURCHASE -> handleGroupPurchase(checkoutSession, escrow);
            default -> log.warn("Unknown session type: {}", checkoutSession.getSessionType());
        }

        log.info("✓ Payment success callback completed");
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
        log.warn("Session Type: {}", checkoutSession.getSessionType());
        log.warn("Error Message: {}", errorMessage);
        log.warn("Payment Status: {}", result.getStatus());
        log.warn("Error Code: {}", result.getErrorCode());
        log.warn("Attempt #: {}", checkoutSession.getPaymentAttemptCount());

        // Release held inventory
        if (checkoutSession.getInventoryHeld() != null && checkoutSession.getInventoryHeld()) {
            log.warn("[TODO] Release held inventory");
            for (CheckoutSessionEntity.CheckoutItem item : checkoutSession.getItems()) {
                log.warn("  → Release {} units of product {} ({})",
                        item.getQuantity(),
                        item.getProductName(),
                        item.getProductId());
            }
        }

        // Send failure notification
        log.warn("[TODO] Send payment failure notification to buyer");

        // Check retry attempts
        int attemptCount = checkoutSession.getPaymentAttemptCount();
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
            CheckoutSessionEntity checkoutSession,
            PaymentResult result) {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║          PAYMENT PENDING CALLBACK                          ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("Checkout Session ID: {}", checkoutSession.getSessionId());
        log.info("Session Type: {}", checkoutSession.getSessionType());
        log.info("External Reference: {}", result.getExternalReference());
        log.info("Payment URL: {}", result.getPaymentUrl());
        log.info("USSD Code: {}", result.getUssdCode());

        log.info("[TODO] Send payment pending notification with instructions");
        log.info("[TODO] Extend session expiration for external payment");
        log.info("[TODO] Schedule periodic payment status check");

        log.info("✓ Payment pending callback completed");
    }

    // ========================================
    // PRIVATE HANDLER METHODS
    // ========================================

    private void handleInstallmentPayment(
            CheckoutSessionEntity checkoutSession,
            EscrowAccountEntity escrow) throws BadRequestException, ItemNotFoundException {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║          INSTALLMENT PAYMENT HANDLER                       ║");
        log.info("╚════════════════════════════════════════════════════════════╝");

        log.info("Processing installment payment...");
        log.info("  Checkout Session: {}", checkoutSession.getSessionId());
        log.info("  Down Payment: {} TZS", escrow.getTotalAmount());
        log.info("  Installment Plan: {}", checkoutSession.getSelectedInstallmentPlanId());

        // Create installment agreement
        var agreement = installmentService.createInstallmentAgreement(checkoutSession);

        log.info("✓ Installment agreement created successfully");
        log.info("  Agreement Number: {}", agreement.getAgreementNumber());
        log.info("  Agreement ID: {}", agreement.getAgreementId());
        log.info("  Status: {}", agreement.getAgreementStatus());
        log.info("  Number of Payments: {}", agreement.getNumberOfPayments());
        log.info("  First Payment Date: {}", agreement.getFirstPaymentDate());
        log.info("  Monthly Payment: {} TZS", agreement.getMonthlyPaymentAmount());
        log.info("  Total Amount: {} TZS", agreement.getTotalAmount());
        log.info("  Fulfillment: {}", agreement.getFulfillmentTiming());

        if (agreement.getOrderId() != null) {
            log.info("  Order Created: {}", agreement.getOrderId());
            log.info("  Product will ship immediately (IMMEDIATE fulfillment)");
        } else {
            log.info("  Order: Not created yet (AFTER_PAYMENT fulfillment)");
            log.info("  Product will ship after final payment");
        }

        log.info("[TODO] Send installment agreement confirmation email");
        log.info("[TODO] Send payment schedule to customer");
        log.info("[TODO] Set up payment reminders");
    }

    private void handleRegularDirectly(
            CheckoutSessionEntity checkoutSession,
            EscrowAccountEntity escrow) {

        checkoutSession.setStatus(CheckoutSessionStatus.PAYMENT_COMPLETED);
        checkoutSessionRepo.save(checkoutSession);


        log.info("✓ Checkout session status updated to PAYMENT_COMPLETED");
        //log.info("✓ Checkout session items{}", checkoutSession.getItems());
        log.info("Handling REGULAR_DIRECTLY purchase");
        log.info("[TODO] Create order from checkout session");
        log.info("[TODO] Update inventory");
        log.info("[TODO] Send order confirmation email");
    }

    private void handleRegularCart(
            CheckoutSessionEntity checkoutSession,
            EscrowAccountEntity escrow) {

        log.info("Handling REGULAR_CART purchase");
        log.info("[TODO] Create order from cart");
        log.info("[TODO] Clear cart for customer");
        log.info("[TODO] Update inventory");
        log.info("[TODO] Send order confirmation email");
    }

    private void handleGroupPurchase(
            CheckoutSessionEntity checkoutSession,
            EscrowAccountEntity escrow) throws BadRequestException, ItemNotFoundException {

        log.info("Handling GROUP_PURCHASE");

        if (checkoutSession.getGroupIdToBeJoined() != null) {
            groupPurchaseService.joinGroup(checkoutSession);
            log.info("✓ Customer joined existing group");
        } else {
            groupPurchaseService.createGroupInstance(checkoutSession);
            log.info("✓ New group instance created");
        }
    }
}