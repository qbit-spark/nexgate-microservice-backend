package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.utils;

import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.entity.RegistrationTokenEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Validator for registration tokens
 * Ensures token is valid for scanner registration
 */
@Slf4j
@Component
public class RegistrationTokenValidator {

    /**
     * Validate registration token for use
     *
     * @param token The token to validate
     * @throws IllegalStateException if token is invalid
     */
    public void validateForRegistration(RegistrationTokenEntity token) {
        if (token == null) {
            throw new IllegalArgumentException("Registration token not found");
        }

        // Check if already used
        if (token.getUsed()) {
            log.warn("Registration token already used: {}", token.getToken());
            throw new IllegalStateException(
                    "Registration token has already been used"
            );
        }

        // Check if expired
        if (token.isExpired()) {
            log.warn("Registration token expired: {}", token.getToken());
            throw new IllegalStateException(
                    "Registration token has expired"
            );
        }

        // Check if event exists
        if (token.getEvent() == null) {
            log.error("Registration token has no associated event: {}", token.getToken());
            throw new IllegalStateException(
                    "Registration token is invalid (no event)"
            );
        }

        log.debug("Registration token validated successfully: {}", token.getToken());
    }

    /**
     * Check if token is valid for registration (non-throwing version)
     *
     * @param token The token to check
     * @return true if valid, false otherwise
     */
    public boolean isValidForRegistration(RegistrationTokenEntity token) {
        try {
            validateForRegistration(token);
            return true;
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get validation error message
     *
     * @param token The token to check
     * @return Error message if invalid, null if valid
     */
    public String getValidationError(RegistrationTokenEntity token) {
        if (token == null) {
            return "Token not found";
        }

        if (token.getUsed()) {
            return "Token has already been used";
        }

        if (token.isExpired()) {
            long secondsAgo = Instant.now().getEpochSecond() - token.getExpiresAt().getEpochSecond();
            return String.format("Token expired %d seconds ago", secondsAgo);
        }

        if (token.getEvent() == null) {
            return "Token has no associated event";
        }

        return null; // Valid
    }
}