package org.nextgate.nextgatebackend.e_commerce.installment_purchase.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.repo.CheckoutSessionRepo;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.entity.InstallmentAgreementEntity;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.FulfillmentTiming;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.events.InstallmentAgreementCompletedEvent;
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
 * Listens for InstallmentAgreementCompletedEvent and creates orders
 * for AFTER_PAYMENT fulfillment timing.
 *
 * Flow:
 * 1. User completes final payment (or early payoff)
 * 2. Agreement status → COMPLETED
 * 3. Event published
 * 4. This listener creates order asynchronously
 * 5. Product ships after order creation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InstallmentCompletionOrderListener {

    private final OrderService orderService;
    private final InstallmentAgreementRepo agreementRepo;
    private final CheckoutSessionRepo checkoutSessionRepo;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAgreementCompleted(InstallmentAgreementCompletedEvent event) {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║   HANDLING AGREEMENT COMPLETION (ASYNC)                    ║");
        log.info("║   Phase: AFTER_COMMIT                                      ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("Agreement ID: {}", event.getAgreementId());
        log.info("Completed At: {}", event.getCompletedAt());
        log.info("Is Early Payoff: {}", event.isEarlyPayoff());

        try {
            // ========================================
            // 1. FETCH AGREEMENT
            // ========================================
            InstallmentAgreementEntity agreement = agreementRepo
                    .findById(event.getAgreementId())
                    .orElse(null);

            if (agreement == null) {
                log.error("✗ Agreement not found: {}", event.getAgreementId());
                return;
            }

            log.info("Agreement Details:");
            log.info("  Agreement Number: {}", agreement.getAgreementNumber());
            log.info("  Customer: {}", agreement.getCustomer().getUserName());
            log.info("  Product: {}", agreement.getProductName());
            log.info("  Fulfillment Timing: {}", agreement.getFulfillmentTiming());
            log.info("  Existing Order ID: {}", agreement.getOrderId());

            // ========================================
            // 2. CHECK IF AFTER_PAYMENT FULFILLMENT
            // ========================================
            if (agreement.getFulfillmentTiming() != FulfillmentTiming.AFTER_PAYMENT) {
                log.info("✓ Fulfillment is IMMEDIATE - order already created");
                log.info("  Existing Order ID: {}", agreement.getOrderId());
                return;
            }

            log.info("✓ Fulfillment is AFTER_PAYMENT - order should be created now");

            // ========================================
            // 3. IDEMPOTENCY CHECK
            // ========================================
            if (agreement.getOrderId() != null) {
                log.info("✓ Order already exists: {}", agreement.getOrderId());
                log.info("  Skipping order creation (idempotency check)");
                return;
            }

            // Check checkout session for orders
            CheckoutSessionEntity session = checkoutSessionRepo
                    .findById(agreement.getCheckoutSessionId())
                    .orElse(null);

            if (session != null &&
                    session.getCreatedOrderIds() != null &&
                    !session.getCreatedOrderIds().isEmpty()) {
                log.info("✓ Order already created via session: {}",
                        session.getCreatedOrderIds());

                // Update agreement with order ID
                UUID orderId = session.getCreatedOrderIds().getFirst();
                agreement.setOrderId(orderId);
                agreementRepo.save(agreement);

                log.info("✓ Agreement updated with existing order ID: {}", orderId);
                return;
            }

            log.info("✓ No existing order - proceeding with creation");

            // ========================================
            // 4. CREATE ORDER WITH RETRY
            // ========================================
            log.info("Creating order for AFTER_PAYMENT fulfillment...");

            boolean orderCreated = createOrderWithRetry(
                    agreement.getCheckoutSessionId(),
                    agreement.getAgreementId(),
                    3  // max retry attempts
            );

            if (orderCreated) {
                log.info("╔════════════════════════════════════════════════════════════╗");
                log.info("║   ✓ ORDER CREATED SUCCESSFULLY FOR AFTER_PAYMENT          ║");
                log.info("╚════════════════════════════════════════════════════════════╝");
                log.info("Agreement: {}", agreement.getAgreementNumber());
                log.info("Customer: {}", agreement.getCustomer().getEmail());
                log.info("Product: {}", agreement.getProductName());

                if (event.isEarlyPayoff()) {
                    log.info("Note: Order created after early payoff");
                } else {
                    log.info("Note: Order created after final scheduled payment");
                }

            } else {
                log.error("╔════════════════════════════════════════════════════════════╗");
                log.error("║   ✗ ORDER CREATION FAILED AFTER RETRIES                   ║");
                log.error("╚════════════════════════════════════════════════════════════╝");
                log.error("⚠️ MANUAL INTERVENTION REQUIRED");
                log.error("  Agreement: {}", agreement.getAgreementNumber());
                log.error("  Customer: {}", agreement.getCustomer().getEmail());
                log.error("  Product: {}", agreement.getProductName());
                log.error("  Action: Manually create order from checkout session");
                log.error("  Checkout Session: {}", agreement.getCheckoutSessionId());
            }

        } catch (Exception e) {
            log.error("╔════════════════════════════════════════════════════════════╗");
            log.error("║   ⚠ ERROR IN ORDER CREATION LISTENER ⚠                    ║");
            log.error("╚════════════════════════════════════════════════════════════╝");
            log.error("Agreement: {}", event.getAgreementId(), e);
            log.error("Stack trace:", e);
        }
    }

    /**
     * Create order with exponential backoff retry
     */
    private boolean createOrderWithRetry(
            UUID checkoutSessionId,
            UUID agreementId,
            int maxAttempts) {

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("Order creation attempt {}/{}", attempt, maxAttempts);

                // ========================================
                // CREATE ORDER VIA ORDER SERVICE
                // ========================================
                List<UUID> orderIds = orderService
                        .createOrdersFromCheckoutSession(checkoutSessionId);

                if (orderIds == null || orderIds.isEmpty()) {
                    throw new RuntimeException("No orders created from checkout session");
                }

                UUID orderId = orderIds.get(0);
                log.info("✓ Order created: {}", orderId);

                // ========================================
                // UPDATE AGREEMENT WITH ORDER ID
                // ========================================
                InstallmentAgreementEntity agreement = agreementRepo
                        .findById(agreementId)
                        .orElse(null);

                if (agreement != null) {
                    agreement.setOrderId(orderId);
                    agreementRepo.save(agreement);
                    log.info("✓ Agreement linked to order");
                } else {
                    log.warn("⚠ Agreement not found for update: {}", agreementId);
                }

                // ========================================
                // UPDATE CHECKOUT SESSION
                // ========================================
                CheckoutSessionEntity session = checkoutSessionRepo
                        .findById(checkoutSessionId)
                        .orElse(null);

                if (session != null) {
                    session.addCreatedOrderId(orderId);
                    // Status should already be COMPLETED from payment
                    if (session.getStatus() != CheckoutSessionStatus.COMPLETED) {
                        session.setStatus(CheckoutSessionStatus.COMPLETED);
                    }
                    checkoutSessionRepo.save(session);
                    log.info("✓ Checkout session updated with order ID");
                } else {
                    log.warn("⚠ Checkout session not found: {}", checkoutSessionId);
                }

                return true;

            } catch (Exception e) {
                log.warn("Attempt {}/{} failed: {}",
                        attempt, maxAttempts, e.getMessage());

                if (attempt < maxAttempts) {
                    // Exponential backoff: 2^attempt seconds
                    long waitTime = (long) Math.pow(2, attempt) * 1000;
                    log.info("Waiting {} ms before retry...", waitTime);

                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry interrupted");
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