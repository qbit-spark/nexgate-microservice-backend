package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for registration token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationTokenResponse {

    /**
     * Token ID
     */
    private UUID tokenId;

    /**
     * The registration token string
     * Example: "REG-ABC12345-XYZ67890"
     */
    private String token;

    /**
     * Event ID this token is for
     */
    private UUID eventId;

    /**
     * Event name
     */
    private String eventName;

    /**
     * Scanner name (if provided)
     */
    private String scannerName;

    /**
     * When token expires
     */
    private Instant expiresAt;

    /**
     * Validity duration in minutes
     */
    private Integer validityMinutes;

    /**
     * Remaining validity in seconds
     */
    private Long remainingSeconds;

    /**
     * QR code data (URL/deep link for scanner app)
     * Example: "scannerapp://register?token=REG-ABC-XYZ"
     */
    private String qrCodeData;

    /**
     * Whether token is still valid
     */
    private Boolean isValid;

    /**
     * Whether token has been used
     */
    private Boolean used;
}