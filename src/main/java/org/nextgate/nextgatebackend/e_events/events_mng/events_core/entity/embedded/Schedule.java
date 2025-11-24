package org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.embedded;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventDayEntity;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {

    @Column(name = "start_date_time", nullable = false)
    private ZonedDateTime startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private ZonedDateTime endDateTime;

    @Column(name = "timezone", length = 50)
    private String timezone;

    // For MULTI_DAY events
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EventDayEntity> days = new ArrayList<>();

    // For RECURRING events
    @Embedded
    private Recurrence recurrence;
}
