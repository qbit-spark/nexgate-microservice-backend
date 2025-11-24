package org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRequest {

    private ZonedDateTime startDateTime;

    private ZonedDateTime endDateTime;

    private String timezone;

    // For MULTI_DAY events
    @Valid
    @Builder.Default
    private List<EventDayRequest> days = new ArrayList<>();

}