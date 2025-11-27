package org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.service;

import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.entity.EventCheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.entity.EventBookingOrderEntity;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing event booking orders
 * Created after successful payment processing
 */
public interface EventBookingOrderService {

    /**
     * Main orchestration method - creates a complete booking order
     * Called by EventPaymentCompletedListener after payment success
     */
    void createBookingOrder(EventCheckoutSessionEntity eventCheckoutSession) throws ItemNotFoundException;


    EventBookingOrderEntity getBookingById(UUID bookingId) throws ItemNotFoundException, AccessDeniedException;

    List<EventBookingOrderEntity> getMyBookings() throws ItemNotFoundException;
}