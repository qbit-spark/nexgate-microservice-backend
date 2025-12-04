package org.nextgate.nextgatebackend.e_commerce.checkout_session.listeners;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.ProductCheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionType;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.repo.ProductCheckoutSessionRepo;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.service.GroupPurchaseService;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.service.ProductOrderService;
import org.nextgate.nextgatebackend.financial_system.payment_processing.events.PaymentCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductPaymentCompletedListener {

    private final ProductOrderService productOrderService;
    private final ProductCheckoutSessionRepo productCheckoutSessionRepo;
    private final GroupPurchaseService groupPurchaseService;

    @EventListener
    @Async
    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {

        // Filter: Only handle PRODUCT domain
        if (!event.isProductDomain()) {
            return;
        }

        if (event.getEscrow() == null) {
            log.info("🆓 FREE product order detected - proceeding with order creation");
        }

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║   PRODUCT PAYMENT COMPLETED - ORDER CREATION               ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("Session: {} | Escrow: {}",
                event.getCheckoutSessionId(), event.getEscrow().getEscrowNumber());

        try {
            if (!(event.getSession() instanceof ProductCheckoutSessionEntity productSession)) {
                log.error("Invalid session type for ProductPaymentCompletedListener");
                return;
            }

            // Skip installment - handled in callback
            if (productSession.getSessionType() == CheckoutSessionType.INSTALLMENT) {
                log.info("INSTALLMENT - order handled by InstallmentService");
                return;
            }

            // Check if order already exists
            if (productSession.getOrderId() != null) {
                log.warn("Order already exists: {}", productSession.getOrderId());
                return;
            }

            // Determine if order should be created now
            boolean shouldCreateOrder = shouldCreateOrderNow(productSession);

            if (!shouldCreateOrder) {
                log.info("Order creation deferred for: {}", productSession.getSessionType());

                // Check group completion
                if (productSession.getSessionType() == CheckoutSessionType.GROUP_PURCHASE) {
                    handleGroupPurchaseCompletion(productSession);
                }

                return;
            }

            log.info("Creating order...");

            // Create order with retry
            boolean orderCreated = createOrderWithRetry(
                    productSession.getSessionId(),
                    productSession.getSessionType().name(),
                    3
            );

            if (orderCreated) {
                log.info("✓ Order created successfully");

                ProductCheckoutSessionEntity updatedSession = productCheckoutSessionRepo
                        .findById(productSession.getSessionId())
                        .orElse(null);

                if (updatedSession != null && updatedSession.getStatus() == CheckoutSessionStatus.PAYMENT_COMPLETED) {
                    updatedSession.setStatus(CheckoutSessionStatus.COMPLETED);
                    productCheckoutSessionRepo.save(updatedSession);
                    log.info("✓ Session status updated to COMPLETED");
                }

            } else {
                log.error("✗ Failed to create order after retries");
            }

            log.info("✓ Product payment handling complete");

        } catch (Exception e) {
            log.error("Error in product payment listener", e);
        }
    }

    private boolean shouldCreateOrderNow(ProductCheckoutSessionEntity session) {
        return switch (session.getSessionType()) {
            case REGULAR_DIRECTLY, REGULAR_CART -> true;
            case GROUP_PURCHASE, INSTALLMENT -> false;
        };
    }

    private void handleGroupPurchaseCompletion(ProductCheckoutSessionEntity session) {
        UUID groupInstanceId = session.getGroupIdToBeJoined();

        if (groupInstanceId != null) {
            log.info("Checking group completion: {}", groupInstanceId);

            try {
                groupPurchaseService.checkAndPublishGroupCompletion(groupInstanceId);
            } catch (Exception e) {
                log.error("Error checking group completion", e);
            }
        }
    }

    private boolean createOrderWithRetry(UUID sessionId, String sessionType, int maxAttempts) {

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("Order creation attempt {}/{}", attempt, maxAttempts);

                List<UUID> orderIds = productOrderService.createOrdersFromCheckoutSession(sessionId);
                UUID orderId = orderIds.getFirst();

                log.info("✓ Order created: {}", orderId);

                return true;

            } catch (Exception e) {
                log.warn("Attempt {}/{} failed: {}", attempt, maxAttempts, e.getMessage());

                if (attempt < maxAttempts) {
                    try {
                        long waitTime = (long) Math.pow(2, attempt) * 1000;
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }

        return false;
    }
}