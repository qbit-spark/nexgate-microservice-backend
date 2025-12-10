package org.nextgate.nextgatebackend.financial_system.payment_processing.strategy.impl;


import org.nextgate.nextgatebackend.financial_system.payment_processing.contract.PayableCheckoutSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.e_commerce.cart_service.service.CartService;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.ProductCheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.repo.ProductCheckoutSessionRepo;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.service.GroupPurchaseService;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.service.InstallmentService;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.financial_system.payment_processing.strategy.PostPaymentHandler;
import org.nextgate.nextgatebackend.globe_enums.CheckoutSessionsDomains;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductPostPaymentHandler implements PostPaymentHandler {

    private final GroupPurchaseService groupPurchaseService;
    private final InstallmentService installmentService;
    private final ProductCheckoutSessionRepo productCheckoutSessionRepo;
    private final CartService cartService;

    @Override
    public void handlePostPayment(
            PayableCheckoutSession session,
            EscrowAccountEntity escrow) throws BadRequestException, ItemNotFoundException {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║     PRODUCT POST-PAYMENT HANDLER                           ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("Session: {} | Escrow: {}", session.getSessionId(), escrow.getEscrowNumber());

        if (!(session instanceof ProductCheckoutSessionEntity productSession)) {
            throw new IllegalArgumentException("Invalid session type for ProductPostPaymentHandler");
        }

        switch (productSession.getSessionType()) {
            case INSTALLMENT -> handleInstallmentPayment(productSession);
            case REGULAR_DIRECTLY -> handleRegularDirectly(productSession);
            case REGULAR_CART -> handleRegularCart(productSession);
            case GROUP_PURCHASE -> handleGroupPurchase(productSession);
            default -> log.warn("Unknown session type: {}", productSession.getSessionType());
        }

        log.info("✓ Product post-payment completed");
    }

    @Override
    public CheckoutSessionsDomains getSupportedDomain() {
        return CheckoutSessionsDomains.PRODUCT;
    }

    private void handleInstallmentPayment(ProductCheckoutSessionEntity session)
            throws BadRequestException, ItemNotFoundException {

        log.info("Processing installment payment...");
        log.info("  Installment Plan: {}", session.getSelectedInstallmentPlanId());

        var agreement = installmentService.createInstallmentAgreement(session);

        log.info("✓ Installment agreement created: {}", agreement.getAgreementNumber());
        log.info("  Number of Payments: {}", agreement.getNumberOfPayments());
        log.info("  Monthly Payment: {} TZS", agreement.getMonthlyPaymentAmount());
        log.info("  Fulfillment: {}", agreement.getFulfillmentTiming());

        if (agreement.getOrderId() != null) {
            log.info("  Order Created: {} (IMMEDIATE fulfillment)", agreement.getOrderId());
        } else {
            log.info("  Order: Not created yet (AFTER_PAYMENT fulfillment)");
        }
    }

    private void handleRegularDirectly(ProductCheckoutSessionEntity session) {
        log.info("Handling REGULAR_DIRECTLY purchase");

        session.setStatus(CheckoutSessionStatus.PAYMENT_COMPLETED);
        productCheckoutSessionRepo.save(session);

        log.info("✓ Session status updated to PAYMENT_COMPLETED");
    }

    private void handleRegularCart(ProductCheckoutSessionEntity session) throws ItemNotFoundException {
        log.info("Handling REGULAR_CART purchase");

        session.setStatus(CheckoutSessionStatus.PAYMENT_COMPLETED);
        productCheckoutSessionRepo.save(session);

        cartService.clearCart();

        log.info("✓ Session status updated to PAYMENT_COMPLETED");
        log.info("✓ Cart cleared");
    }

    private void handleGroupPurchase(ProductCheckoutSessionEntity session)
            throws BadRequestException, ItemNotFoundException {

        log.info("Handling GROUP_PURCHASE");

        if (session.getGroupIdToBeJoined() != null) {
            groupPurchaseService.joinGroup(session);
            log.info("✓ Customer joined existing group");
        } else {
            groupPurchaseService.createGroupInstance(session);
            log.info("✓ New group instance created");
        }
    }
}