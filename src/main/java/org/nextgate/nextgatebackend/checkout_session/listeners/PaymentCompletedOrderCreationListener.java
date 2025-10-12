package org.nextgate.nextgatebackend.checkout_session.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionType;
import org.nextgate.nextgatebackend.checkout_session.events.PaymentCompletedEvent;
import org.nextgate.nextgatebackend.checkout_session.repo.CheckoutSessionRepo;
import org.nextgate.nextgatebackend.order_mng_service.service.OrderService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Listens for PaymentCompletedEvent and creates orders when appropriate.
 * <p>
 * Handles order creation for:
 * - REGULAR_DIRECTLY: Creates order immediately
 * - REGULAR_CART: Creates order immediately
 * - INSTALLMENT (IMMEDIATE): Creates order immediately
 * - INSTALLMENT (AFTER_PAYMENT): Defers (waits for completion event)
 * - GROUP_PURCHASE: Defers (waits for group completion event)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedOrderCreationListener {

    private final OrderService orderService;
    private final CheckoutSessionRepo checkoutSessionRepo;

    @EventListener
    @Async
    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   HANDLING PAYMENT COMPLETION - ORDER CREATION        ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("🎉 Payment completed! Let's create an order!");
        log.info("Checkout Session: {}", event.getCheckoutSessionId());
        log.info("Session Type: {}", event.getSession().getSessionType());
        log.info("Transaction ID: {}", event.getTransactionId());
        log.info("Completed At: {}", event.getCompletedAt());

        try {
            CheckoutSessionEntity session = event.getSession();

            // CHECK IF ORDER ALREADY EXISTS
//            if (session.getCreatedOrderId() != null) {
//                log.warn("Order already exists: {}", session.getCreatedOrderId());
//                log.warn("Skipping order creation");
//                return;
//            }


            // DETERMINE IF ORDER SHOULD BE CREATED NOW
            boolean shouldCreateOrder = shouldCreateOrderNow(session);

            if (!shouldCreateOrder) {
                log.info("Order creation deferred for session type: {}",
                        session.getSessionType());
                log.info("Order will be created when:");

                if (session.getSessionType() == CheckoutSessionType.GROUP_PURCHASE) {
                    log.info("  → Group reaches full capacity");
                } else if (session.getSessionType() == CheckoutSessionType.INSTALLMENT) {
                    log.info("  → All installment payments completed");
                }

                return;
            }

            log.info("Order should be created now - proceeding...");

            // CREATE ORDER WITH RETRY
            boolean orderCreated = createOrderWithRetry(
                    session.getSessionId(),
                    session.getSessionType().name(),
                    3
            );

            if (orderCreated) {
                log.info("✓ Order created successfully");

                CheckoutSessionEntity updatedSession =
                        checkoutSessionRepo.findById(session.getSessionId())
                                .orElse(null);

                if (updatedSession != null) {
                    if (updatedSession.getStatus() == CheckoutSessionStatus.PAYMENT_COMPLETED) {
                        updatedSession.setStatus(CheckoutSessionStatus.COMPLETED);
                        checkoutSessionRepo.save(updatedSession);

                        log.info("✓ Session status updated to COMPLETED");
                    }

                    log.info("✓ Order ID: {}", updatedSession.getCreatedOrderId());
                }

            } else {
                log.error("✗ Failed to create order after retries");
            }

            log.info("╔════════════════════════════════════════════════════════╗");
            log.info("║   ORDER CREATION HANDLING COMPLETE                     ║");
            log.info("╚════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            log.error("╔════════════════════════════════════════════════════════╗");
            log.error("║   ⚠ CRITICAL ERROR IN ORDER CREATION ⚠                ║");
            log.error("╚════════════════════════════════════════════════════════╝");
            log.error("Session: {}", event.getCheckoutSessionId(), e);
        }
    }

    // ========================================
    // HELPER: DETERMINE IF ORDER SHOULD BE CREATED NOW
    // ========================================

    private boolean shouldCreateOrderNow(CheckoutSessionEntity session) {

        return switch (session.getSessionType()) {

            case REGULAR_DIRECTLY, REGULAR_CART -> {
                log.debug("Direct/Cart purchase - create order now");
                yield true;
            }

            case INSTALLMENT -> {
                CheckoutSessionEntity.InstallmentConfiguration config =
                        session.getInstallmentConfig();

                if (config == null) {
                    log.warn("Installment session missing config");
                    yield false;
                }

                boolean isImmediate = "IMMEDIATE".equals(config.getFulfillmentTiming());

                log.debug("Installment - fulfillment: {} - create order: {}",
                        config.getFulfillmentTiming(), isImmediate);

                yield isImmediate;
            }

            case GROUP_PURCHASE -> {
                log.debug("Group purchase - defer order creation until group completes");
                yield false;
            }
        };
    }

    // ========================================
    // HELPER: CREATE ORDER WITH RETRY
    // ========================================

    private boolean createOrderWithRetry(
            UUID checkoutSessionId,
            String sessionType,
            int maxAttempts
    ) {

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("Order creation attempt {}/{} for session type: {}",
                        attempt, maxAttempts, sessionType);

                List<UUID> orderIds = orderService
                        .createOrdersFromCheckoutSession(checkoutSessionId);

                UUID orderId = orderIds.get(0);

                log.info("✓ Order created: {}", orderId);

                return true;

            } catch (Exception e) {
                log.warn("Attempt {}/{} failed: {}",
                        attempt, maxAttempts, e.getMessage());

                if (attempt < maxAttempts) {
                    // Wait before retry (exponential backoff)
                    try {
                        long waitTime = (long) Math.pow(2, attempt) * 1000; // 2s, 4s, 8s
                        log.debug("Waiting {}ms before retry", waitTime);
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Retry wait interrupted");
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