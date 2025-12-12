package org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventFormat;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventVisibility;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventDraftRequest {

    @NotBlank(message = "Event title is required")
    @Size(min = 3, max = 200)
    private String title;

    @NotNull(message = "Category is required")
    private UUID categoryId;

    private EventVisibility eventVisibility;

    @NotNull(message = "Event format is required")
    private EventFormat eventFormat;

    @Size(max = 5000)
    private String description;

}