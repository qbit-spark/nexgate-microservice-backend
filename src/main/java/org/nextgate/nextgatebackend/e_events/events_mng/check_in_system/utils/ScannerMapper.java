package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.utils;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.entity.ScannerEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads.ScannerResponse;
import org.nextgate.nextgatebackend.globe_crypto.RSAKeyService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScannerMapper {

    private final RSAKeyService rsaKeyService;  // Add this

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
                .publicKey(rsaKeyService.getPublicKeyBase64(scanner.getEvent().getRsaKeys()))
                .revocationReason(scanner.getRevocationReason())
                .build();
    }
}