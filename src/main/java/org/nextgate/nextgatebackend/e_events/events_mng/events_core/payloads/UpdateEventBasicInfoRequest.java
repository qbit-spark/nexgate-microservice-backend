package org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads;

import jakarta.validation.constraints.Size;
import lombok.*;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventFormat;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventVisibility;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventBasicInfoRequest {

    @Size(min = 3, max = 200)
    private String title;

    @Size(min = 15, max = 5000)
    private String description;

    private UUID categoryId;
    private EventVisibility eventVisibility;
    private EventFormat eventFormat;
}