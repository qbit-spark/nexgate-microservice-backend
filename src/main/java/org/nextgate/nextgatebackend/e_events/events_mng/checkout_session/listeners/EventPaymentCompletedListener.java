package org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.listeners;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.entity.EventCheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.repo.EventCheckoutSessionRepo;
import org.nextgate.nextgatebackend.financial_system.payment_processing.events.PaymentCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPaymentCompletedListener {

    private final EventCheckoutSessionRepo eventCheckoutSessionRepo;
    // TODO: Inject BookingService when implemented

    @EventListener
    @Async
    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {

        // Filter: Only handle EVENT domain
        if (!event.isEventDomain()) {
            return;
        }

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║   EVENT PAYMENT COMPLETED - BOOKING CREATION               ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("Session: {} | Escrow: {}",
                event.getCheckoutSessionId(), event.getEscrow().getEscrowNumber());

        try {
            if (!(event.getSession() instanceof EventCheckoutSessionEntity eventSession)) {
                log.error("Invalid session type for EventPaymentCompletedListener");
                return;
            }

            // Check if booking already exists
            if (eventSession.getCreatedBookingOrderId() != null) {
                log.warn("Booking already exists: {}", eventSession.getCreatedBookingOrderId());
                return;
            }

            log.info("Creating booking...");
            log.info("  Event: {}", eventSession.getEventId());
            log.info("  Total Tickets: {}", eventSession.getTicketDetails().getTotalQuantity());
            log.info("  Buyer Tickets: {}", eventSession.getTicketDetails().getTicketsForBuyer());

            // TODO: Create booking order
            log.info("[TODO] Create booking order");
            log.info("[TODO] Reserve tickets");
            log.info("[TODO] Generate QR codes");

            if (eventSession.getTicketDetails().getSendTicketsToAttendees()) {
                log.info("[TODO] Send tickets to other attendees");
            }

            // Update session status
            eventSession.setStatus(CheckoutSessionStatus.COMPLETED);
            eventCheckoutSessionRepo.save(eventSession);

            log.info("✓ Event payment handling complete");

        } catch (Exception e) {
            log.error("Error in event payment listener", e);
        }
    }
}
