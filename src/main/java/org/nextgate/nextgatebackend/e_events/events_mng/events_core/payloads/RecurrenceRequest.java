package org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.RecurrenceFrequency;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurrenceRequest {

    private RecurrenceFrequency frequency;

    private Integer interval;

    @Builder.Default
    private Set<String> daysOfWeek = new HashSet<>();

    private Integer dayOfMonth;

    private LocalDate recurrenceStartDate;

    private LocalDate recurrenceEndDate;

    @Builder.Default
    private Set<LocalDate> exceptions = new HashSet<>();
}
