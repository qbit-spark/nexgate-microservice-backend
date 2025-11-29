package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.utils;

import org.springframework.stereotype.Component;

@Component
public class DeviceFingerprintValidator {

    private static final int MIN_FINGERPRINT_LENGTH = 10;
    private static final int MAX_FINGERPRINT_LENGTH = 255;

    /**
     * Validates the device fingerprint format.
     * Throws IllegalArgumentException if validation fails.
     *
     * @param deviceFingerprint the unique identifier string for the device
     */
    public void validate(String deviceFingerprint) {
        if (deviceFingerprint == null || deviceFingerprint.isBlank()) {
            throw new IllegalArgumentException("Device fingerprint cannot be null or empty");
        }

        if (deviceFingerprint.length() < MIN_FINGERPRINT_LENGTH) {
            throw new IllegalArgumentException("Device fingerprint is too short (minimum " + MIN_FINGERPRINT_LENGTH + " characters)");
        }

        if (deviceFingerprint.length() > MAX_FINGERPRINT_LENGTH) {
            throw new IllegalArgumentException("Device fingerprint is too long (maximum " + MAX_FINGERPRINT_LENGTH + " characters)");
        }
    }
}