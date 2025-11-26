package org.nextgate.nextgatebackend.financial_system.payment_processing.strategy.impl;


import com.qbitspark.jikoexpress.financial_system.payment_processing.contract.PayableCheckoutSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.entity.EventCheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.repo.EventCheckoutSessionRepo;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.financial_system.payment_processing.strategy.PostPaymentHandler;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPostPaymentHandler implements PostPaymentHandler {

    private final EventCheckoutSessionRepo eventCheckoutSessionRepo;
    // TODO: Inject BookingService when implemented

    @Override
    public void handlePostPayment(
            PayableCheckoutSession session,
            EscrowAccountEntity escrow) throws BadRequestException, ItemNotFoundException {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║     EVENT POST-PAYMENT HANDLER                             ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("Session: {} | Escrow: {}", session.getSessionId(), escrow.getEscrowNumber());

        if (!(session instanceof EventCheckoutSessionEntity eventSession)) {
            throw new IllegalArgumentException("Invalid session type for EventPostPaymentHandler");
        }

        handleEventBooking(eventSession);

        log.info("✓ Event post-payment completed");
    }

    @Override
    public String getSupportedDomain() {
        return "EVENT";
    }

    private void handleEventBooking(EventCheckoutSessionEntity session) {
        log.info("Processing event booking...");
        log.info("  Event: {}", session.getEventId());
        log.info("  Tickets: {}", session.getTicketDetails().getTotalQuantity());
        log.info("  Buyer Tickets: {}", session.getTicketDetails().getTicketsForBuyer());

        if (session.getTicketDetails().getOtherAttendees() != null) {
            log.info("  Other Attendees: {}", session.getTicketDetails().getOtherAttendees().size());
        }

        session.setStatus(CheckoutSessionStatus.PAYMENT_COMPLETED);
        eventCheckoutSessionRepo.save(session);

        log.info("✓ Session status updated to PAYMENT_COMPLETED");

        // TODO: Create booking order
        log.info("[TODO] Create booking order");
        log.info("[TODO] Reserve tickets");
        log.info("[TODO] Generate QR codes");

        if (session.getTicketDetails().getSendTicketsToAttendees()) {
            log.info("[TODO] Send tickets to other attendees");
        }
    }
}