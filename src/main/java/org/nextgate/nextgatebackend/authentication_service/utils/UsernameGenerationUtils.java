package org.nextgate.nextgatebackend.authentication_service.utils;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class UsernameGenerationUtils {

    private final AccountRepo accountRepo;

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SUFFIX_LENGTH = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a unique username from email
     * Format: emailPrefix-randomSuffix
     * Example: office@qbitspark.com → office-a1B2c3D
     */
    public String generateUniqueUsernameFromEmail(String email) {
        String baseUsername = extractUsernameFromEmail(email);
        String uniqueUsername;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            String suffix = generateRandomSuffix();
            uniqueUsername = baseUsername + "-" + suffix;
            attempts++;

            if (attempts > maxAttempts) {
                // Fallback: use longer suffix if too many collisions
                suffix = generateRandomSuffix(SUFFIX_LENGTH + 2);
                uniqueUsername = baseUsername + "-" + suffix;
                break;
            }
        } while (accountRepo.existsByUserName(uniqueUsername));

        return uniqueUsername;
    }

    /**
     * Validates if a custom username is available
     * Rules:
     * - Must be 3-30 characters
     * - Only alphanumeric, underscore, hyphen allowed
     * - Cannot start with number
     * - Must be unique
     */
    public UsernameValidationResult validateCustomUsername(String username) {
        // Check length
        if (username == null || username.trim().isEmpty()) {
            return UsernameValidationResult.invalid("Username cannot be empty");
        }

        String trimmedUsername = username.trim().toLowerCase();

        if (trimmedUsername.length() < 3) {
            return UsernameValidationResult.invalid("Username must be at least 3 characters long");
        }

        if (trimmedUsername.length() > 30) {
            return UsernameValidationResult.invalid("Username must be at most 30 characters long");
        }

        // Check format - only alphanumeric, underscore, hyphen
        if (!trimmedUsername.matches("^[a-zA-Z][a-zA-Z0-9_-]*$")) {
            return UsernameValidationResult.invalid("Username must start with a letter and contain only letters, numbers, underscores, and hyphens");
        }

        // Check reserved words
        if (isReservedUsername(trimmedUsername)) {
            return UsernameValidationResult.invalid("This username is reserved and cannot be used");
        }

        // Check uniqueness
        if (accountRepo.existsByUserName(trimmedUsername)) {
            return UsernameValidationResult.invalid("This username is already taken");
        }

        return UsernameValidationResult.valid(trimmedUsername);
    }

    /**
     * Extracts username part from email
     * office@qbitspark.com → office
     */
    private String extractUsernameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        String prefix = email.substring(0, email.indexOf("@")).toLowerCase();

        // Clean the prefix - remove any non-alphanumeric characters except underscore
        prefix = prefix.replaceAll("[^a-zA-Z0-9_]", "");

        // Ensure it's not empty after cleaning
        if (prefix.isEmpty()) {
            prefix = "user";
        }

        // Ensure it doesn't start with a number
        if (Character.isDigit(prefix.charAt(0))) {
            prefix = "u" + prefix;
        }

        // Limit length
        if (prefix.length() > 15) {
            prefix = prefix.substring(0, 15);
        }

        return prefix;
    }

    /**
     * Generates random suffix of specified length
     */
    private String generateRandomSuffix() {
        return generateRandomSuffix(SUFFIX_LENGTH);
    }

    private String generateRandomSuffix(int length) {
        StringBuilder suffix = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            suffix.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return suffix.toString();
    }

    /**
     * Checks if username is reserved
     */
    private boolean isReservedUsername(String username) {
        String[] reservedWords = {
                "admin", "administrator", "root", "system", "api", "www", "mail",
                "support", "help", "info", "contact", "service", "user", "guest",
                "null", "undefined", "test", "demo", "example", "nexgate",
                "buildwise", "official", "staff", "moderator", "bot"
        };

        for (String reserved : reservedWords) {
            if (username.equalsIgnoreCase(reserved)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Result class for username validation
     */
    public static class UsernameValidationResult {
        private final boolean valid;
        private final String username;
        private final String errorMessage;

        private UsernameValidationResult(boolean valid, String username, String errorMessage) {
            this.valid = valid;
            this.username = username;
            this.errorMessage = errorMessage;
        }

        public static UsernameValidationResult valid(String username) {
            return new UsernameValidationResult(true, username, null);
        }

        public static UsernameValidationResult invalid(String errorMessage) {
            return new UsernameValidationResult(false, null, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getUsername() {
            return username;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}