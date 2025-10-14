package org.nextgate.nextgatebackend.installment_purchase.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentAgreementEntity;
import org.nextgate.nextgatebackend.installment_purchase.events.InstallmentAgreementCreatedEvent;
import org.nextgate.nextgatebackend.installment_purchase.repo.InstallmentAgreementRepo;
import org.nextgate.nextgatebackend.order_mng_service.service.OrderService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    @EventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAgreementCreated(InstallmentAgreementCreatedEvent event) {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║   HANDLING INSTALLMENT AGREEMENT CREATED (ASYNC)           ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("Agreement ID: {}", event.getAgreementId());
        log.info("Agreement Number: {}", event.getAgreement().getAgreementNumber());
        log.info("Requires Immediate Order: {}", event.isRequiresImmediateOrder());

        try {
            // ========================================
            // CHECK IF ORDER SHOULD BE CREATED
            // ========================================

            if (!event.isRequiresImmediateOrder()) {
                log.info("AFTER_PAYMENT fulfillment - no order needed now");
                return;
            }

            log.info("IMMEDIATE fulfillment - creating order asynchronously...");

            // ========================================
            // CHECK IF ORDER ALREADY EXISTS
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

                InstallmentAgreementEntity updatedAgreement = agreementRepo
                        .findById(event.getAgreementId())
                        .orElse(null);

                if (updatedAgreement != null && updatedAgreement.getOrderId() != null) {
                    log.info("✓ Agreement updated with order ID: {}",
                            updatedAgreement.getOrderId());
                }

            } else {
                log.error("✗ Failed to create order after retries");
                log.error("⚠️ MANUAL INTERVENTION REQUIRED");
                log.error("  Agreement: {}", agreement.getAgreementNumber());
            }

            log.info("╔════════════════════════════════════════════════════════════╗");
            log.info("║   ORDER CREATION COMPLETE                                  ║");
            log.info("╚════════════════════════════════════════════════════════════╝");

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
            int maxAttempts
    ) {

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("Order creation attempt {}/{}", attempt, maxAttempts);

                List<UUID> orderIds = orderService
                        .createOrdersFromCheckoutSession(checkoutSessionId);

                UUID orderId = orderIds.get(0);
                log.info("✓ Order created: {}", orderId);

                InstallmentAgreementEntity agreement = agreementRepo
                        .findById(agreementId)
                        .orElse(null);

                if (agreement != null) {
                    agreement.setOrderId(orderId);
                    agreementRepo.save(agreement);
                    log.info("✓ Agreement linked to order");
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