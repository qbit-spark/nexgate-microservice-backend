package org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.repo;

import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.entity.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketRepo extends JpaRepository<TicketEntity, UUID> {

}
