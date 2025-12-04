package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.entity.RegistrationTokenEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads.GenerateRegistrationTokenRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.repo.RegistrationTokenRepository;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.service.RegistrationTokenService;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.utils.RegistrationTokenValidator;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventsRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationTokenServiceImpl implements RegistrationTokenService {

    private final RegistrationTokenRepository registrationTokenRepo;
    private final EventsRepo eventsRepo;
    private final AccountRepo accountRepo;
    private final RegistrationTokenValidator tokenValidator;

    @Value("${scanner.token.registration.expiry.minutes}")
    private int validityMinutes;

    @Override
    @Transactional
    public RegistrationTokenEntity generateRegistrationToken(GenerateRegistrationTokenRequest request) throws ItemNotFoundException, AccessDeniedException {

        log.info("Generating registration token for event: {}", request.getEventId());

        // Get an authenticated user
        AccountEntity currentUser = getAuthenticatedAccount();

        // Fetch event
        EventEntity event = eventsRepo.findByIdAndIsDeletedFalse(request.getEventId())
                .orElseThrow(() -> new ItemNotFoundException("Event not found: " + request.getEventId()));

        // Validate user is event organizer
        validateEventOwnership(event, currentUser);

        // Validate event has RSA keys (required for ticket validation)
        if (!event.hasActiveKeys()) {
            log.error("Event does not have RSA keys: {}", event.getId());
            throw new IllegalStateException(
                    "Event must be published and have RSA keys before generating scanner registration tokens"
            );
        }

        // Generate token
        String tokenString = generateTokenString();


        Instant expiresAt = Instant.now().plus(validityMinutes, ChronoUnit.MINUTES);

        // Create a token entity
        RegistrationTokenEntity token = RegistrationTokenEntity.builder()
                .token(tokenString)
                .event(event)
                .scannerName(request.getScannerName())
                .validityMinutes(validityMinutes)
                .expiresAt(expiresAt)
                .createdBy(currentUser)
                .build();

        RegistrationTokenEntity savedToken = registrationTokenRepo.save(token);

        log.info("Registration token generated: {} for event: {} (expires in {} minutes)",
                tokenString, event.getTitle(), validityMinutes);

        return savedToken;
    }

    @Override
    public RegistrationTokenEntity findByToken(String token) throws ItemNotFoundException {
        log.debug("Finding registration token: {}", token);

        return registrationTokenRepo.findByToken(token)
                .orElseThrow(() -> new ItemNotFoundException("Registration token not found or expired"));
    }

    @Override
    public List<RegistrationTokenEntity> getTokensForEvent(UUID eventId) throws ItemNotFoundException, AccessDeniedException {
        log.debug("Fetching all registration tokens for event: {}", eventId);

        AccountEntity currentUser = getAuthenticatedAccount();

        EventEntity event = eventsRepo.findByIdAndIsDeletedFalse(eventId)
                .orElseThrow(() -> new ItemNotFoundException("Event not found: " + eventId));

        validateEventOwnership(event, currentUser);

        return registrationTokenRepo.findByEvent(event);
    }

    @Override
    public List<RegistrationTokenEntity> getActiveTokensForEvent(UUID eventId) throws AccessDeniedException, ItemNotFoundException {
        log.debug("Fetching active registration tokens for event: {}", eventId);

        AccountEntity currentUser = getAuthenticatedAccount();

        EventEntity event = eventsRepo.findByIdAndIsDeletedFalse(eventId)
                .orElseThrow(() -> new ItemNotFoundException("Event not found: " + eventId));

        validateEventOwnership(event, currentUser);

        return registrationTokenRepo.findByEventAndUsedAndExpiresAtAfter(
                event,
                false,
                Instant.now()
        );
    }

    @Override
    @Transactional
    public void markTokenAsUsed(RegistrationTokenEntity token, String scannerId) {
        log.info("Marking token as used: {} by scanner: {}", token.getToken(), scannerId);

        token.markAsUsed(scannerId);
        registrationTokenRepo.save(token);
    }

    @Override
    @Transactional
    public void deleteExpiredTokens() {
        log.info("Deleting expired registration tokens");

        // Delete tokens expired more than 24 hours ago
        Instant cutoffTime = Instant.now().minus(24, ChronoUnit.HOURS);
        registrationTokenRepo.deleteByExpiresAtBefore(cutoffTime);

        log.info("Expired registration tokens deleted");
    }

    // ========================================
    // PRIVATE HELPER METHODS
    // ========================================

    /**
     * Get authenticated account
     */
    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
        }
        throw new ItemNotFoundException("User not authenticated");
    }

    /**
     * Validate that current user is the event organizer
     */
    private void validateEventOwnership(EventEntity event, AccountEntity user) throws AccessDeniedException {
        if (!event.getOrganizer().getId().equals(user.getId())) {
            log.warn("User {} attempted to generate token for event {} they don't own",
                    user.getUserName(), event.getId());
            throw new AccessDeniedException("Only event organizer can generate registration tokens");
        }
    }

    /**
     * Generate unique registration token string
     * Format: REG-{8-char-uuid}-{8-char-uuid}
     */
    private String generateTokenString() {
        String uuid1 = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String uuid2 = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("REG-%s-%s", uuid1, uuid2);
    }
}