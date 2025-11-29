package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for generating a registration token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateRegistrationTokenRequest {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    /**
     * Optional scanner name/description
     * Example: "Gate A - Main Entrance", "VIP Section"
     * If provided, will be used as the default scanner name during registration
     */
    private String scannerName;

}