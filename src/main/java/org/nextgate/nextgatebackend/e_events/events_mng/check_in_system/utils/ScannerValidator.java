package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.utils;

import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.entity.ScannerEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.enums.ScannerStatus;
import org.springframework.stereotype.Component;

/**
 * Validator for scanner operations
 * Ensures scanner is in valid state for operations
 */
@Slf4j
@Component
public class ScannerValidator {

    /**
     * Validate scanner is active and can perform scans
     *
     * @param scanner The scanner to validate
     * @throws IllegalStateException if scanner cannot perform scans
     */
    public void validateForScanning(ScannerEntity scanner) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner not found");
        }

        if (scanner.getStatus() == ScannerStatus.REVOKED) {
            String reason = scanner.getRevocationReason() != null
                    ? scanner.getRevocationReason()
                    : "Unknown reason";
            log.warn("Scanner is revoked: {} - Reason: {}", scanner.getScannerId(), reason);
            throw new IllegalStateException("Scanner has been revoked: " + reason);
        }

        if (scanner.getEvent() == null) {
            throw new IllegalStateException("Scanner has no associated event");
        }

        log.debug("Scanner validated for scanning: {}", scanner.getScannerId());
    }

    /**
     * Validate scanner name
     *
     * @param name The scanner name to validate
     * @throws IllegalArgumentException if name is invalid
     */
    public void validateScannerName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Scanner name cannot be empty");
        }

        if (name.length() < 3) {
            throw new IllegalArgumentException("Scanner name too short (minimum 3 characters)");
        }

        if (name.length() > 200) {
            throw new IllegalArgumentException("Scanner name too long (maximum 200 characters)");
        }
    }

    /**
     * Validate device fingerprint matches scanner's stored fingerprint
     * Used to detect if credentials were copied to different device
     *
     * @param scanner The scanner
     * @param providedFingerprint The fingerprint from current request
     * @throws IllegalStateException if fingerprints don't match
     */
    public void validateDeviceFingerprint(ScannerEntity scanner, String providedFingerprint) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner not found");
        }

        if (providedFingerprint == null || providedFingerprint.isBlank()) {
            throw new IllegalArgumentException("Device fingerprint not provided");
        }

        if (!scanner.getDeviceFingerprint().equals(providedFingerprint)) {
            log.error("Device fingerprint mismatch for scanner: {}. Expected: {}, Got: {}",
                    scanner.getScannerId(),
                    scanner.getDeviceFingerprint(),
                    providedFingerprint);

            throw new IllegalStateException(
                    "Device fingerprint mismatch. Credentials may have been copied to different device."
            );
        }

        log.debug("Device fingerprint validated for scanner: {}", scanner.getScannerId());
    }

    /**
     * Check if the scanner is valid for scanning (non-throwing version)
     *
     * @param scanner The scanner to check
     * @return true if valid, false otherwise
     */
    public boolean isValidForScanning(ScannerEntity scanner) {
        try {
            validateForScanning(scanner);
            return true;
        } catch (Exception e) {
            log.debug("Scanner validation failed: {}", e.getMessage());
            return false;
        }
    }
}