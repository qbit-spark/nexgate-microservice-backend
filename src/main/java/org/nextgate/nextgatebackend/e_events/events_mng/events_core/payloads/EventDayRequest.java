package org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDayRequest {

    private LocalDate date;

    private LocalTime startTime;

    private LocalTime endTime;

    private String description;

    private Integer dayOrder;
}