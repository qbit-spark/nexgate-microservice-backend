package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for validating/checking in a ticket
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateTicketRequest {

    /**
     * JWT token from ticket QR code
     */
    @NotBlank(message = "JWT token is required")
    private String jwtToken;

    /**
     * Scanner ID performing the validation
     */
    @NotBlank(message = "Scanner ID is required")
    private String scannerId;

    /**
     * Device fingerprint (for security validation)
     */
    @NotBlank(message = "Device fingerprint is required")
    private String deviceFingerprint;

    /**
     * Optional: Gate/location name where scan happened
     */
    private String checkInLocation;
}