package org.nextgate.nextgatebackend.financial_system.payment_processing.callbacks.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.cart_service.entity.CartEntity;
import org.nextgate.nextgatebackend.cart_service.service.CartService;
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
    private final CartService cartService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentSuccess(
            CheckoutSessionEntity checkoutSession,
            EscrowAccountEntity escrow) throws BadRequestException, ItemNotFoundException {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║          PAYMENT SUCCESS CALLBACK   (SYNCHRONOUS)          ║");
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
            case INSTALLMENT -> handleInstallmentPayment(checkoutSession);
            case REGULAR_DIRECTLY -> handleRegularDirectly(checkoutSession);
            case REGULAR_CART -> handleRegularCart(checkoutSession);
            case GROUP_PURCHASE -> handleGroupPurchase(checkoutSession);
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
    private void handleInstallmentPayment(CheckoutSessionEntity checkoutSession) throws BadRequestException, ItemNotFoundException {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║     HANDLING INSTALLMENT PAYMENT                           ║");
        log.info("╚════════════════════════════════════════════════════════════╝");

        log.info("Processing installment payment...");
        log.info("  Checkout Session: {}", checkoutSession.getSessionId());
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

    private void handleRegularDirectly(CheckoutSessionEntity checkoutSession) {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║     HANDLING REGULAR_DIRECTLY PURCHASE                     ║");
        log.info("╚════════════════════════════════════════════════════════════╝");

        checkoutSession.setStatus(CheckoutSessionStatus.PAYMENT_COMPLETED);
        checkoutSessionRepo.save(checkoutSession);

        log.info("✓ Checkout session status updated to PAYMENT_COMPLETED");

    }

    private void handleRegularCart(CheckoutSessionEntity checkoutSession) throws ItemNotFoundException {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║     HANDLING REGULAR_CART PURCHASE                         ║");
        log.info("╚════════════════════════════════════════════════════════════╝");

        checkoutSession.setStatus(CheckoutSessionStatus.PAYMENT_COMPLETED);
        checkoutSessionRepo.save(checkoutSession);

        cartService.clearCart();

        log.info("Handling REGULAR_CART purchase");
        log.info("✓ Checkout session status updated to PAYMENT_COMPLETED");
        log.info("[Clear cart for customer");

    }

    private void handleGroupPurchase(CheckoutSessionEntity checkoutSession) throws BadRequestException, ItemNotFoundException {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║     HANDLING GROUP_PURCHASE                                ║");
        log.info("╚════════════════════════════════════════════════════════════╝");

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