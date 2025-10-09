package org.nextgate.nextgatebackend.installment_purchase.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentAgreementEntity;
import org.nextgate.nextgatebackend.installment_purchase.enums.FulfillmentTiming;
import org.nextgate.nextgatebackend.installment_purchase.events.InstallmentAgreementCompletedEvent;
import org.nextgate.nextgatebackend.installment_purchase.repo.InstallmentAgreementRepo;
import org.nextgate.nextgatebackend.order_mng_service.service.OrderService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Listens for InstallmentAgreementCompletedEvent and creates orders
 * for AFTER_PAYMENT fulfillment.
 *
 * Executes asynchronously to avoid blocking the payment processing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InstallmentOrderCreationListener {

    private final OrderService orderService;
    private final InstallmentAgreementRepo agreementRepo;

    @EventListener
    @Async
    @Transactional
    public void onAgreementCompleted(InstallmentAgreementCompletedEvent event) {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   HANDLING AGREEMENT COMPLETION - ORDER CREATION      ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Agreement ID: {}", event.getAgreementId());
        log.info("Agreement Number: {}", event.getAgreement().getAgreementNumber());
        log.info("Customer: {}", event.getAgreement().getCustomer().getUserName());
        log.info("Product: {}", event.getAgreement().getProductName());
        log.info("Completed At: {}", event.getCompletedAt());
        log.info("Early Payoff: {}", event.isEarlyPayoff());

        try {
            InstallmentAgreementEntity agreement = event.getAgreement();

            // ========================================
            // 1. VALIDATE FULFILLMENT TYPE
            // ========================================

            if (agreement.getFulfillmentTiming() != FulfillmentTiming.AFTER_PAYMENT) {
                log.info("Fulfillment type is IMMEDIATE - order already created, skipping");
                return;
            }

            log.info("Fulfillment type: AFTER_PAYMENT - order needs to be created");

            // ========================================
            // 2. CHECK IF ORDER ALREADY EXISTS
            // ========================================

            if (agreement.getOrderId() != null) {
                log.warn("Order already exists: {}", agreement.getOrderId());
                log.warn("Skipping order creation");
                return;
            }

            // ========================================
            // 3. GET CHECKOUT SESSION ID
            // ========================================

            UUID checkoutSessionId = agreement.getCheckoutSessionId();

            if (checkoutSessionId == null) {
                log.error("Agreement has no checkout session ID!");
                log.error("Cannot create order without checkout session");
                // TODO: Alert admins
                return;
            }

            log.info("Checkout Session ID: {}", checkoutSessionId);

            // ========================================
            // 4. CREATE ORDER WITH RETRY
            // ========================================

            boolean orderCreated = createOrderWithRetry(
                    checkoutSessionId,
                    agreement.getAgreementNumber(),
                    3  // max attempts
            );

            if (orderCreated) {
                log.info("✓ Order created successfully");

                // Reload agreement to get updated orderId
                InstallmentAgreementEntity updatedAgreement =
                        agreementRepo.findById(agreement.getAgreementId())
                                .orElse(null);

                if (updatedAgreement != null && updatedAgreement.getOrderId() != null) {
                    log.info("✓ Agreement updated with order ID: {}",
                            updatedAgreement.getOrderId());
                }

            } else {
                log.error("✗ Failed to create order after retries");

                // TODO: Create admin task
                // adminTaskService.createTask(
                //     "Installment Order Creation Failed",
                //     String.format("Agreement %s needs manual order creation",
                //         agreement.getAgreementNumber())
                // );

                // TODO: Alert admins
                // alertService.sendAlert(
                //     AlertLevel.HIGH,
                //     "Installment order creation failed",
                //     details
                // );
            }

            log.info("╔════════════════════════════════════════════════════════╗");
            log.info("║   ORDER CREATION HANDLING COMPLETE                     ║");
            log.info("╚════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            log.error("╔════════════════════════════════════════════════════════╗");
            log.error("║   ⚠ CRITICAL ERROR IN ORDER CREATION ⚠                ║");
            log.error("╚════════════════════════════════════════════════════════╝");
            log.error("Agreement: {} ({})",
                    event.getAgreement().getAgreementNumber(),
                    event.getAgreementId(), e);

            // TODO: Alert admins immediately
            // alertService.sendCriticalAlert(
            //     "Installment order creation failed catastrophically",
            //     event.getAgreementId(),
            //     e
            // );
        }
    }


    // ========================================
    // HELPER METHOD: CREATE ORDER WITH RETRY
    // ========================================

    private boolean createOrderWithRetry(
            UUID checkoutSessionId,
            String agreementNumber,
            int maxAttempts
    ) {

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("Order creation attempt {}/{} for agreement {}",
                        attempt, maxAttempts, agreementNumber);

                List<UUID> orderIds = orderService
                        .createOrdersFromCheckoutSession(checkoutSessionId);

                UUID orderId = orderIds.get(0);

                log.info("✓ Order created: {} for agreement {}",
                        orderId, agreementNumber);

                return true;

            } catch (Exception e) {
                log.warn("Attempt {}/{} failed for agreement {}: {}",
                        attempt, maxAttempts, agreementNumber, e.getMessage());

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
                    log.error("All {} attempts failed for agreement {}",
                            maxAttempts, agreementNumber);
                }
            }
        }

        return false;
    }
}