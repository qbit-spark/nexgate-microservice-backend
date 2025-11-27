package org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.service;

import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.entity.EventCheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.entity.EventBookingOrderEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.entity.TicketEntity;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing event booking orders
 * Created after successful payment processing
 */
public interface EventBookingOrderService {

    /**
     * Main orchestration method - creates complete booking order
     * Called by EventPaymentCompletedListener after payment success
     *
     * @param checkoutSessionId The checkout session ID that was completed
     * @param escrowId Optional escrow ID (null for free tickets)
     * @return The created booking order
     */
    EventBookingOrderEntity createBookingOrder(UUID checkoutSessionId, UUID escrowId) throws ItemNotFoundException;


}