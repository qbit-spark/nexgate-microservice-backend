package org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.repo;

import jakarta.persistence.LockModeType;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.entity.TicketSeriesCounterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketSeriesCounterRepo extends JpaRepository<TicketSeriesCounterEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TicketSeriesCounterEntity> findByTicketTypeId(UUID ticketTypeId);
}