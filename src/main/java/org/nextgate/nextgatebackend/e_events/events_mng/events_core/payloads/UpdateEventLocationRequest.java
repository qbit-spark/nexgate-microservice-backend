package org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads;

import jakarta.validation.Valid;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventLocationRequest {

    @Valid
    private VenueRequest venue;

    @Valid
    private VirtualDetailsRequest virtualDetails;
}