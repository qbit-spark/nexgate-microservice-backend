package org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "ticket_series_counters")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketSeriesCounterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "ticket_type_id", nullable = false, unique = true)
    private UUID ticketTypeId;

    @Column(name = "current_counter", nullable = false)
    private Integer currentCounter;

    @Version
    @Column(name = "version")
    private Long version;

    public Integer getNextCounter() {
        this.currentCounter++;
        return this.currentCounter;
    }
}