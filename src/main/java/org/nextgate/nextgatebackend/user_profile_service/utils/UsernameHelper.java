package org.nextgate.nextgatebackend.user_profile_service.utils;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UsernameHelper {

    private final AccountRepo accountRepo;
    private final ProfileValidationUtils validationUtils;
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generate username suggestions when the desired username is taken
     */
    public List<String> generateUsernameSuggestions(String baseUsername, int maxSuggestions) {
        List<String> suggestions = new ArrayList<>();

        if (baseUsername == null || baseUsername.trim().isEmpty()) {
            return suggestions;
        }

        String cleanBase = baseUsername.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");

        // Try adding numbers
        for (int i = 1; i <= 99 && suggestions.size() < maxSuggestions; i++) {
            String suggestion = cleanBase + i;
            if (isUsernameAvailable(suggestion)) {
                suggestions.add(suggestion);
            }
        }

        // Try adding random suffixes
        String[] suffixes = {"user", "pro", "star", "plus", "new", "cool", "top"};
        for (String suffix : suffixes) {
            if (suggestions.size() >= maxSuggestions) break;
            String suggestion = cleanBase + suffix;
            if (isUsernameAvailable(suggestion)) {
                suggestions.add(suggestion);
            }
        }

        // Try adding random numbers at the end
        for (int i = 0; i < 5 && suggestions.size() < maxSuggestions; i++) {
            String randomNum = String.valueOf(random.nextInt(9999) + 1000);
            String suggestion = cleanBase + randomNum;
            if (isUsernameAvailable(suggestion)) {
                suggestions.add(suggestion);
            }
        }

        return suggestions;
    }

    /**
     * Check if username is available and valid
     */
    public boolean isUsernameAvailable(String username) {
        return validationUtils.isValidUsername(username) &&
                !validationUtils.isReservedUsername(username) &&
                !accountRepo.existsByUserName(username);
    }

    /**
     * Validate username with detailed feedback
     */
    public UsernameValidationResult validateUsernameDetailed(String username) {
        if (username == null || username.trim().isEmpty()) {
            return new UsernameValidationResult(
                    false, false, "Username cannot be empty",
                    new ValidationDetails(false, false, true, true, "Username is required")
            );
        }

        String trimmed = username.trim();

        boolean correctLength = trimmed.length() >= 3 && trimmed.length() <= 30;
        boolean validFormat = validationUtils.isValidUsername(trimmed);
        boolean notReserved = !validationUtils.isReservedUsername(trimmed);
        boolean notTaken = !accountRepo.existsByUserName(trimmed);

        boolean isValid = correctLength && validFormat && notReserved;
        boolean isAvailable = isValid && notTaken;

        String message;
        if (!correctLength) {
            message = "Username must be between 3 and 30 characters";
        } else if (!validFormat) {
            message = "Username must start with a letter and contain only letters, numbers, underscores, and hyphens";
        } else if (!notReserved) {
            message = "This username is reserved and cannot be used";
        } else if (!notTaken) {
            message = "This username is already taken";
        } else {
            message = "Username is available";
        }

        ValidationDetails details = new ValidationDetails(
                correctLength, validFormat, notReserved, notTaken,
                "Username must start with a letter and be 3-30 characters long"
        );

        return new UsernameValidationResult(isAvailable, isValid, message, details);
    }

    public static class UsernameValidationResult {
        private final boolean available;
        private final boolean valid;
        private final String message;
        private final ValidationDetails details;

        public UsernameValidationResult(boolean available, boolean valid, String message, ValidationDetails details) {
            this.available = available;
            this.valid = valid;
            this.message = message;
            this.details = details;
        }

        public boolean isAvailable() { return available; }
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public ValidationDetails getDetails() { return details; }
    }

    public static class ValidationDetails {
        private final boolean correctLength;
        private final boolean validFormat;
        private final boolean notReserved;
        private final boolean notTaken;
        private final String formatRequirement;

        public ValidationDetails(boolean correctLength, boolean validFormat, boolean notReserved,
                                 boolean notTaken, String formatRequirement) {
            this.correctLength = correctLength;
            this.validFormat = validFormat;
            this.notReserved = notReserved;
            this.notTaken = notTaken;
            this.formatRequirement = formatRequirement;
        }

        public boolean isCorrectLength() { return correctLength; }
        public boolean isValidFormat() { return validFormat; }
        public boolean isNotReserved() { return notReserved; }
        public boolean isNotTaken() { return notTaken; }
        public String getFormatRequirement() { return formatRequirement; }
    }
}