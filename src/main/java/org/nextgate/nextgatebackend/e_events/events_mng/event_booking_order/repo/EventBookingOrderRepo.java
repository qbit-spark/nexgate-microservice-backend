package org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.repo;


import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.entity.EventBookingOrderEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.enums.BookingStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for EventBookingOrder
 * Contains only essential query methods
 */
@Repository
public interface EventBookingOrderRepo extends JpaRepository<EventBookingOrderEntity, UUID> {

    // Find booking by ID and customer (ownership verification)
    Optional<EventBookingOrderEntity> findByBookingIdAndCustomer(UUID bookingId, AccountEntity customer);

    // Find all bookings for a customer
    List<EventBookingOrderEntity> findByCustomerOrderByBookedAtDesc(AccountEntity customer);

    // Find bookings for a specific event by customer
    List<EventBookingOrderEntity> findByEventAndCustomer(EventEntity event, AccountEntity customer);

    // Find bookings by event and status
    List<EventBookingOrderEntity> findByEventAndStatus(EventEntity event, BookingStatus status);

    // Find booking by checkout session
    Optional<EventBookingOrderEntity> findByCheckoutSessionId(UUID checkoutSessionId);

    // In EventBookingOrderRepo:

    @Query(value = """
        SELECT * FROM event_booking_orders 
        WHERE booked_tickets::text LIKE CONCAT('%"ticketInstanceId":"', :ticketInstanceId, '"%')
        LIMIT 1
        """, nativeQuery = true)
    Optional<EventBookingOrderEntity> findByTicketInstanceId(@Param("ticketInstanceId") String ticketInstanceId);
}