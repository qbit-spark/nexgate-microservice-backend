package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.entity.RegistrationTokenEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.entity.ScannerEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.enums.ScannerStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads.RegisterScannerRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.repo.ScannerRepository;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.service.RegistrationTokenService;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.service.ScannerService;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.utils.DeviceFingerprintValidator;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.utils.RegistrationTokenValidator;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.utils.ScannerValidator;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventsRepo;
import org.nextgate.nextgatebackend.globe_crypto.RSAKeyService;
import org.nextgate.nextgatebackend.globe_crypto.TicketJWTService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScannerServiceImpl implements ScannerService {

    private final ScannerRepository scannerRepo;
    private final EventsRepo eventsRepo;
    private final AccountRepo accountRepo;
    private final RegistrationTokenService registrationTokenService;
    private final RSAKeyService rsaKeyService;
    private final TicketJWTService ticketJWTService;
    private final DeviceFingerprintValidator fingerprintValidator;
    private final RegistrationTokenValidator tokenValidator;
    private final ScannerValidator scannerValidator;

    @Override
    @Transactional
    public ScannerEntity registerScanner(RegisterScannerRequest request)
            throws IllegalStateException, ItemNotFoundException {

        log.info("Registering scanner with token: {}", request.getRegistrationToken());

        // 1. Validate device fingerprint format
        fingerprintValidator.validate(request.getDeviceFingerprint());

        // 2. Validate scanner name
        scannerValidator.validateScannerName(request.getScannerName());

        // 3. Find and validate registration token
        RegistrationTokenEntity token = registrationTokenService.findByToken(request.getRegistrationToken());
        tokenValidator.validateForRegistration(token);

        EventEntity event = token.getEvent();

        // 4. Handle duplicate device scenarios
        handleDuplicateDeviceScenarios(event, request.getDeviceFingerprint());

        // 5. Generate scanner credentials (JWT)
        String scannerId = UUID.randomUUID().toString();
        String scannerCredentials = generateScannerCredentials(scannerId, event);

        // 6. Determine scanner name (use token's name if provided, otherwise use request name)
        String scannerName = token.getScannerName() != null && !token.getScannerName().isBlank()
                ? token.getScannerName()
                : request.getScannerName();

        // 7. Create a scanner entity
        ScannerEntity scanner = ScannerEntity.builder()
                .scannerId(scannerId)
                .name(scannerName)
                .event(event)
                .credentials(scannerCredentials)
                .deviceFingerprint(request.getDeviceFingerprint())
                .deviceInfo(request.getDeviceInfo())
                .status(ScannerStatus.ACTIVE)
                .createdBy(token.getCreatedBy())
                .build();

        ScannerEntity savedScanner = scannerRepo.save(scanner);

        // 8. Mark token as used
        registrationTokenService.markTokenAsUsed(token, scannerId);

        log.info("Scanner registered successfully: {} for event: {}", scannerId, event.getTitle());

        return savedScanner;
    }

    @Override
    public ScannerEntity getByScannerId(String scannerId) throws ItemNotFoundException {
        log.debug("Fetching scanner: {}", scannerId);

        return scannerRepo.findByScannerId(scannerId)
                .orElseThrow(() -> new ItemNotFoundException("Scanner not found: " + scannerId));
    }

    @Override
    public List<ScannerEntity> getScannersForEvent(UUID eventId) throws ItemNotFoundException, AccessDeniedException {
        log.debug("Fetching all scanners for event: {}", eventId);

        AccountEntity currentUser = getAuthenticatedAccount();

        EventEntity event = eventsRepo.findByIdAndIsDeletedFalse(eventId)
                .orElseThrow(() -> new ItemNotFoundException("Event not found: " + eventId));

        validateEventOwnership(event, currentUser);

        return scannerRepo.findByEvent(event);
    }


    @Override
    @Transactional
    public void revokeScanner(String scannerId, String reason) throws ItemNotFoundException, AccessDeniedException {
        log.warn("Revoking scanner: {} for reason: {}", scannerId, reason);

        AccountEntity currentUser = getAuthenticatedAccount();

        ScannerEntity scanner = getByScannerId(scannerId);

        // Validate user owns the event
        validateEventOwnership(scanner.getEvent(), currentUser);

        // Revoke scanner
        scanner.revoke(reason, currentUser);
        scannerRepo.save(scanner);

        log.info("Scanner revoked: {}", scannerId);
    }

    // ========================================
    // PRIVATE HELPER METHODS
    // ========================================

    /**
     * New Rule: One device → Only ONE ACTIVE scanner at a time (across all events)
     * If a device tries to register again → revoke any existing ACTIVE scanner it owns
     */
    private void handleDuplicateDeviceScenarios(EventEntity event, String deviceFingerprint)
            throws IllegalStateException {

        log.debug("Checking for existing ACTIVE scanners with fingerprint: {}", deviceFingerprint);

        // Find ANY active scanner with this device fingerprint (across all events)
        List<ScannerEntity> activeScanners = scannerRepo.findByDeviceFingerprintAndStatus(
                deviceFingerprint, ScannerStatus.ACTIVE);

        if (activeScanners.isEmpty()) {
            log.debug("No active scanner found for this device - allowing registration");
            return;
        }

        // Revoke ALL previously active scanners for this device
        for (ScannerEntity oldScanner : activeScanners) {
            String oldEventTitle = oldScanner.getEvent() != null ? oldScanner.getEvent().getTitle() : "Unknown";

            log.warn("Device already has ACTIVE scanner: {} for event: {}. Revoking it automatically.",
                    oldScanner.getScannerId(), oldEventTitle);

            oldScanner.revoke(
                    "Automatically revoked: Device registered as new scanner for event '" + event.getTitle() + "'",
                    null // or pass the current user if available
            );
            scannerRepo.save(oldScanner);

            log.info("Previous scanner revoked: {}", oldScanner.getScannerId());
        }

    }


    /**
     * Generate scanner credentials (JWT token)
     * Contains: scannerId, eventId, expiration (1 year)
     */
    private String generateScannerCredentials(String scannerId, EventEntity event) {
        log.debug("Generating scanner credentials for: {}", scannerId);

        // Create JWT payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("scannerId", scannerId);
        payload.put("eventId", event.getId().toString());
        payload.put("type", "scanner_credential");
        payload.put("iat", Instant.now().getEpochSecond());
        payload.put("exp", Instant.now().plus(365, ChronoUnit.DAYS).getEpochSecond()); // 1 year

        // Sign with event's private key
        // Note: For scanner credentials, we could use a separate signing key
        // For now, using event's key for simplicity

        return ticketJWTService.generateTicketJWT(
                buildScannerJWTData(scannerId, event),
                event.getRsaKeys()
        );
    }

    /**
     * Build JWT data for scanner credentials
     */
    private TicketJWTService.TicketJWTData buildScannerJWTData(String scannerId, EventEntity event) {
        return TicketJWTService.TicketJWTData.builder()
                .ticketInstanceId(UUID.fromString(scannerId)) // Using scannerId as ticketInstanceId
                .eventId(event.getId())
                .eventName(event.getTitle())
                .eventStartDateTime(event.getStartDateTime())
                .validFrom(event.getStartDateTime())
                .validUntil(event.getEndDateTime().plusDays(7)) // Scanner valid for 7 days after event
                .build();
    }

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
            log.warn("User {} attempted to access scanners for event {} they don't own",
                    user.getUserName(), event.getId());
            throw new AccessDeniedException("Only event organizer can manage scanners");
        }
    }
}