package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterScannerRequest {

    /**
     * The one-time registration token generated for this scanner
     */
    @NotBlank(message = "Registration token is required")
    private String registrationToken;

    /**
     * Unique fingerprint/identifier of the device (e.g. hardware ID, MAC-based hash, etc.)
     * Used to prevent duplicate registrations from the same physical device
     */
    @NotBlank(message = "Device fingerprint is required")
    private String deviceFingerprint;

    /**
     * Human-readable name for the scanner (e.g. "Entrance Gate A", "VIP Check-in")
     * Can be overridden by the name set on the registration token
     */
    @NotBlank(message = "Scanner name is required")
    private String scannerName;

    /**
     * Optional additional information about the device (OS, version, model, etc.)
     * Usually sent as JSON string or free-form text
     */
    private String deviceInfo;
}