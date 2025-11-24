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
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventType;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventVisibility;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventRequest {

    // ========== BASIC INFO (Always required) ==========
    @NotBlank(message = "Event title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    @Size(min = 15, message = "Description must not be less than 15 characters")
    @NotBlank(message = "Event description is required")
    private String description;

    @NotNull(message = "Category is required")
    private UUID categoryId;

    @NotNull(message = "Event visibility is required")
    private EventVisibility eventVisibility;

    @NotNull(message = "Event type is required")
    private EventType eventType;

    @NotNull(message = "Event format is required")
    private EventFormat eventFormat;

    // ========== SCHEDULE (Validated in service) ==========
    @Valid
    private ScheduleRequest schedule;

    // ========== LOCATION DETAILS (Validated in service based on format) ==========
    @Valid
    private VenueRequest venue;

    @Valid
    private VirtualDetailsRequest virtualDetails;

    // ========== MEDIA (Optional) ==========
    @Valid
    private MediaRequest media;

    // ========== LINKS (Optional) ==========
    @Builder.Default
    private List<UUID> linkedProductIds = new ArrayList<>();

    @Builder.Default
    private List<UUID> linkedShopIds = new ArrayList<>();

}