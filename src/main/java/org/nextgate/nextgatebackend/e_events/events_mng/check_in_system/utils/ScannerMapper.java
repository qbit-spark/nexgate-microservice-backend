package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.utils;

import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.entity.ScannerEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads.ScannerResponse;
import org.springframework.stereotype.Component;

@Component
public class ScannerMapper {

    public ScannerResponse toResponse(ScannerEntity scanner) {
        return ScannerResponse.builder()
                .scannerId(scanner.getScannerId())
                .name(scanner.getName())
                .eventId(scanner.getEvent().getId())
                .eventName(scanner.getEvent().getTitle())
                .status(scanner.getStatus())
                .deviceFingerprint(scanner.getDeviceFingerprint())
                .createdAt(scanner.getCreatedAt())
                .credentials(scanner.getCredentials()) // This might be null if not fetched or transient
                .revocationReason(scanner.getRevocationReason())
                .build();
    }
}