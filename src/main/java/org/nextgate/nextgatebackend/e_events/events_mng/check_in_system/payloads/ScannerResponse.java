package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.enums.ScannerStatus;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScannerResponse {
    private String scannerId;
    private String name;
    private UUID eventId;
    private String eventName;
    private ScannerStatus status;
    private String deviceFingerprint;
    private Instant createdAt;
    private String credentials; // Only populated on initial registration
    private String publicKey;  // Event's public key for ticket verification
    private String revocationReason;
}