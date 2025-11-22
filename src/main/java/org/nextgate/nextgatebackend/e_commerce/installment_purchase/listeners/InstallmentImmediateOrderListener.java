package org.nextgate.nextgatebackend.e_commerce.installment_purchase.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.repo.CheckoutSessionRepo;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.entity.InstallmentAgreementEntity;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.events.InstallmentAgreementCreatedEvent;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.repo.InstallmentAgreementRepo;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.service.OrderService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

/**
 * Listens for InstallmentAgreementCreatedEvent and creates orders
 * asynchronously for IMMEDIATE fulfillment.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InstallmentImmediateOrderListener {

    private final OrderService orderService;
    private final InstallmentAgreementRepo agreementRepo;
    private final CheckoutSessionRepo checkoutSessionRepo;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // ← CHANGE THIS
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAgreementCreated(InstallmentAgreementCreatedEvent event) {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║   HANDLING INSTALLMENT AGREEMENT CREATED (ASYNC)           ║");
        log.info("║   Phase: AFTER_COMMIT                                      ║"); // ← ADD THIS
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("Agreement ID: {}", event.getAgreementId());

        try {
            // Check if order should be created
            if (!event.isRequiresImmediateOrder()) {
                log.info("AFTER_PAYMENT fulfillment - no order needed now");
                return;
            }

            // ========================================
            // IDEMPOTENCY CHECK
            // ========================================
            InstallmentAgreementEntity agreement = agreementRepo
                    .findById(event.getAgreementId())
                    .orElse(null);

            if (agreement == null) {
                log.error("Agreement not found: {}", event.getAgreementId());
                return;
            }

            if (agreement.getOrderId() != null) {
                log.info("Order already exists: {}", agreement.getOrderId());
                return;
            }

            // Check checkout session
            CheckoutSessionEntity session = checkoutSessionRepo
                    .findById(event.getCheckoutSessionId())
                    .orElse(null);

            if (session != null &&
                    session.getCreatedOrderIds() != null &&
                    !session.getCreatedOrderIds().isEmpty()) {
                log.info("Order already created for session: {}",
                        session.getCreatedOrderIds());
                return;
            }

            log.info("IMMEDIATE fulfillment - creating order asynchronously...");

            // ========================================
            // CREATE ORDER WITH RETRY
            // ========================================
            boolean orderCreated = createOrderWithRetry(
                    event.getCheckoutSessionId(),
                    event.getAgreementId(),
                    3
            );

            if (orderCreated) {
                log.info("✓ Order created successfully for IMMEDIATE fulfillment");
            } else {
                log.error("✗ Failed to create order after retries");
                log.error("⚠️ MANUAL INTERVENTION REQUIRED");
                log.error("  Agreement: {}", agreement.getAgreementNumber());
            }

        } catch (Exception e) {
            log.error("╔════════════════════════════════════════════════════════════╗");
            log.error("║   ⚠ ERROR IN ORDER CREATION ⚠                             ║");
            log.error("╚════════════════════════════════════════════════════════════╝");
            log.error("Agreement: {}", event.getAgreementId(), e);
        }
    }


    private boolean createOrderWithRetry(
            UUID checkoutSessionId,
            UUID agreementId,
            int maxAttempts) {

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("Order creation attempt {}/{}", attempt, maxAttempts);

                List<UUID> orderIds = orderService
                        .createOrdersFromCheckoutSession(checkoutSessionId);

                UUID orderId = orderIds.getFirst();
                log.info("✓ Order created: {}", orderId);

                // Update agreement
                InstallmentAgreementEntity agreement = agreementRepo
                        .findById(agreementId)
                        .orElse(null);

                if (agreement != null) {
                    agreement.setOrderId(orderId);
                    agreementRepo.save(agreement);
                    log.info("✓ Agreement linked to order");
                }

                // ========================================
                // UPDATE CHECKOUT SESSION ← ADD THIS
                // ========================================
                CheckoutSessionEntity session = checkoutSessionRepo
                        .findById(checkoutSessionId)
                        .orElse(null);

                if (session != null) {
                    session.addCreatedOrderId(orderId);
                    session.setStatus(CheckoutSessionStatus.COMPLETED);
                    checkoutSessionRepo.save(session);
                    log.info("✓ Checkout session updated with order ID");
                }

                return true;

            } catch (Exception e) {
                log.warn("Attempt {}/{} failed: {}",
                        attempt, maxAttempts, e.getMessage());

                if (attempt < maxAttempts) {
                    long waitTime = (long) Math.pow(2, attempt) * 1000;
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                } else {
                    log.error("All {} attempts failed", maxAttempts);
                    log.error("Final error: ", e);
                }
            }
        }

        return false;
    }
}