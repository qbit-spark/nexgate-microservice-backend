package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.service;

import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.entity.RegistrationTokenEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads.GenerateRegistrationTokenRequest;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface RegistrationTokenService {
    RegistrationTokenEntity generateRegistrationToken(GenerateRegistrationTokenRequest request) throws ItemNotFoundException, AccessDeniedException;
    RegistrationTokenEntity findByToken(String token) throws ItemNotFoundException;
    List<RegistrationTokenEntity> getTokensForEvent(UUID eventId) throws ItemNotFoundException, AccessDeniedException;

    List<RegistrationTokenEntity> getActiveTokensForEvent(UUID eventId) throws AccessDeniedException, ItemNotFoundException;
    void markTokenAsUsed(RegistrationTokenEntity token, String scannerId);
    void deleteExpiredTokens();
}
