package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.utils;

import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.entity.RegistrationTokenEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads.RegistrationTokenResponse;
import org.springframework.stereotype.Component;

@Component
public class RegistrationTokenMapper {

    public RegistrationTokenResponse toResponse(RegistrationTokenEntity token) {
        return RegistrationTokenResponse.builder()
                .tokenId(token.getId())  // ← Add
                .token(token.getToken())
                .eventId(token.getEvent().getId())
                .eventName(token.getEvent().getTitle())
                .scannerName(token.getScannerName())
                .expiresAt(token.getExpiresAt())
                .validityMinutes(token.getValidityMinutes())  // ← Add
                .remainingSeconds(token.getRemainingValiditySeconds())  // ← Add
                .qrCodeData(buildQRCodeData(token.getToken()))  // ← Add
                .isValid(token.isValid())  // ← Add
                .used(token.getUsed())
                .build();
    }

    private String buildQRCodeData(String token) {
        return String.format("scannerapp://register?token=%s", token);
    }
}