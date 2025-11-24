package org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.embedded;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "event_days")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDay {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity eventEntity;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "day_order")
    private Integer dayOrder;
}