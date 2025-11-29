package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.utils;

import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.entity.RegistrationTokenEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads.RegistrationTokenResponse;
import org.springframework.stereotype.Component;

@Component
public class RegistrationTokenMapper {

    public RegistrationTokenResponse toResponse(RegistrationTokenEntity token) {
        return RegistrationTokenResponse.builder()
                .token(token.getToken())
                .eventId(token.getEvent().getId())
                .eventName(token.getEvent().getTitle())
                .scannerName(token.getScannerName())
                .expiresAt(token.getExpiresAt())
                .used(token.getUsed())
                .build();
    }
}