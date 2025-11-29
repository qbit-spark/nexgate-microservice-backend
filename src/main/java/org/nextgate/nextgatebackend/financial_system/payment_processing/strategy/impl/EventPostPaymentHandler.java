package org.nextgate.nextgatebackend.financial_system.payment_processing.strategy.impl;

import org.nextgate.nextgatebackend.financial_system.payment_processing.contract.PayableCheckoutSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.entity.EventCheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.repo.EventCheckoutSessionRepo;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.financial_system.payment_processing.strategy.PostPaymentHandler;
import org.nextgate.nextgatebackend.globe_enums.CheckoutSessionsDomains;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPostPaymentHandler implements PostPaymentHandler {

    private final EventCheckoutSessionRepo eventCheckoutSessionRepo;

    @Override
    public void handlePostPayment(
            PayableCheckoutSession session,
            EscrowAccountEntity escrow) throws BadRequestException, ItemNotFoundException {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║     EVENT POST-PAYMENT HANDLER (Synchronous)              ║");
        log.info("╚════════════════════════════════════════════════════════════╝");

        if (escrow != null) {
            log.info("Session: {} | Escrow: {}", session.getSessionId(), escrow.getEscrowNumber());
        } else {
            log.info("Session: {} | FREE TICKET", session.getSessionId());
        }

        if (!(session instanceof EventCheckoutSessionEntity eventSession)) {
            throw new IllegalArgumentException("Invalid session type for EventPostPaymentHandler");
        }

        // Only critical session status update (fast, synchronous)
        eventSession.setStatus(CheckoutSessionStatus.PAYMENT_COMPLETED);
        eventCheckoutSessionRepo.save(eventSession);

        log.info("✓ Session status updated to PAYMENT_COMPLETED");
        log.info("✓ Booking creation will be handled asynchronously by EventPaymentCompletedListener");
    }

    @Override
    public CheckoutSessionsDomains getSupportedDomain() {
        return CheckoutSessionsDomains.EVENT;
    }
}