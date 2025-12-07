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

        // ========================================
        // 1. FILTER: ONLY HANDLE PRODUCT DOMAIN
        // ========================================
        if (!event.isProductDomain()) {
            return;
        }

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   PRODUCT PAYMENT COMPLETED LISTENER                  ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Session: {}", event.getCheckoutSessionId());

        try {
            // ========================================
            // 2. VALIDATE SESSION TYPE
            // ========================================
            if (!(event.getSession() instanceof ProductCheckoutSessionEntity productSession)) {
                log.error("Invalid session type for ProductPaymentCompletedListener");
                return;
            }

            log.info("Session Type: {}", productSession.getSessionType());
            log.info("Session Status: {}", productSession.getStatus());

            // ========================================
            // 3. HANDLE BY SESSION TYPE
            // ========================================
            switch (productSession.getSessionType()) {

                case INSTALLMENT -> {
                    log.info("INSTALLMENT - order handled by InstallmentService");
                    // Do nothing - InstallmentService creates order after agreement
                }

                case GROUP_PURCHASE -> {
                    log.info("GROUP_PURCHASE - completion already checked in service");

                    // ✅ NO LONGER CALL checkAndPublishGroupCompletion HERE
                    // Group completion is now handled within the same transaction
                    // in GroupPurchaseServiceImpl.createGroupInstance() / joinGroup()

                    log.info("Group purchase payment completed successfully");
                    log.info("  Group completion was checked in GroupPurchaseService");
                    log.info("  No further action needed here");
                }

                case REGULAR_DIRECTLY, REGULAR_CART -> {
                    log.info("Regular purchase - creating order immediately");

                    // Check if order already exists
                    if (productSession.getOrderId() != null) {
                        log.warn("Order already exists: {}", productSession.getOrderId());
                        return;
                    }

                    // Create order with retry
                    boolean orderCreated = createOrderWithRetry(
                            productSession.getSessionId(),
                            productSession.getSessionType().name(),
                            3
                    );

                    if (orderCreated) {
                        log.info("✓ Order created successfully");

                        // Update session status to COMPLETED
                        ProductCheckoutSessionEntity updatedSession = productCheckoutSessionRepo
                                .findById(productSession.getSessionId())
                                .orElse(null);

                        if (updatedSession != null &&
                                updatedSession.getStatus() == CheckoutSessionStatus.PAYMENT_COMPLETED) {
                            updatedSession.setStatus(CheckoutSessionStatus.COMPLETED);
                            productCheckoutSessionRepo.save(updatedSession);
                            log.info("✓ Session status updated to COMPLETED");
                        }
                    } else {
                        log.error("✗ Failed to create order after retries");
                    }
                }
            }

            log.info("✓ Payment handling complete");
            log.info("╚════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            log.error("╔════════════════════════════════════════════════════════╗");
            log.error("║   ERROR IN PAYMENT LISTENER                           ║");
            log.error("╚════════════════════════════════════════════════════════╝");
            log.error("Session: {}", event.getCheckoutSessionId(), e);
        }
    }

    /**
     * Create order with retry logic
     */
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