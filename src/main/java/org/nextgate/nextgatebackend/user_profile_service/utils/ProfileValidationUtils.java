package org.nextgate.nextgatebackend.user_profile_service.utils;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ProfileValidationUtils {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]*$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    // Pre-compiled patterns for password validation
    private static final Pattern HAS_UPPERCASE = Pattern.compile(".*[A-Z].*");
    private static final Pattern HAS_LOWERCASE = Pattern.compile(".*[a-z].*");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern HAS_SPECIAL = Pattern.compile(".*[@$!%*?&#].*");
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$");

    private static final List<String> RESERVED_USERNAMES = Arrays.asList(
            "admin", "administrator", "root", "system", "api", "www", "mail",
            "support", "help", "info", "contact", "service", "user", "guest",
            "null", "undefined", "test", "demo", "example", "nexgate",
            "buildwise", "official", "staff", "moderator", "bot", "operator",
            "manager", "owner", "public", "private", "security", "abuse"
    );

    // Constants for scoring
    private static final int SCORE_PER_CRITERIA = 20;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int USERNAME_MIN_LENGTH = 3;
    private static final int USERNAME_MAX_LENGTH = 30;

    /**
     * Validate username format and rules
     */
    public boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        String trimmedUsername = username.trim();
        return isValidUsernameLength(trimmedUsername) &&
                USERNAME_PATTERN.matcher(trimmedUsername).matches();
    }

    private boolean isValidUsernameLength(String username) {
        return username.length() >= USERNAME_MIN_LENGTH &&
                username.length() <= USERNAME_MAX_LENGTH;
    }

    /**
     * Check if username is in reserved list
     */
    public boolean isReservedUsername(String username) {
        return username != null && RESERVED_USERNAMES.contains(username.toLowerCase());
    }

    /**
     * Validate phone number format (international format)
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && PHONE_PATTERN.matcher(phoneNumber).matches();
    }

    /**
     * Validate email format
     */
    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Check if password meets strength requirements
     */
    public boolean isStrongPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }
        return STRONG_PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Sanitize input by trimming whitespace
     */
    public String sanitizeInput(String input) {
        return input == null ? null : input.trim();
    }

    /**
     * Check password strength and return detailed analysis
     */
    public PasswordStrengthResult analyzePasswordStrength(String password) {
        if (password == null) {
            return createWeakResult("Password is required");
        }

        PasswordCriteria criteria = evaluatePasswordCriteria(password);
        int score = calculatePasswordScore(criteria);
        String feedback = generatePasswordFeedback(criteria);
        String level = determinePasswordLevel(score);

        String message = feedback.isEmpty() ? "Password meets all requirements" : feedback;
        return new PasswordStrengthResult(score, level, message);
    }

    private PasswordCriteria evaluatePasswordCriteria(String password) {
        return new PasswordCriteria(
                password.length() >= MIN_PASSWORD_LENGTH,
                HAS_UPPERCASE.matcher(password).matches(),
                HAS_LOWERCASE.matcher(password).matches(),
                HAS_DIGIT.matcher(password).matches(),
                HAS_SPECIAL.matcher(password).matches()
        );
    }

    private int calculatePasswordScore(PasswordCriteria criteria) {
        int score = 0;
        if (criteria.hasValidLength) score += SCORE_PER_CRITERIA;
        if (criteria.hasUppercase) score += SCORE_PER_CRITERIA;
        if (criteria.hasLowercase) score += SCORE_PER_CRITERIA;
        if (criteria.hasDigit) score += SCORE_PER_CRITERIA;
        if (criteria.hasSpecial) score += SCORE_PER_CRITERIA;
        return score;
    }

    private String generatePasswordFeedback(PasswordCriteria criteria) {
        StringBuilder feedback = new StringBuilder();

        if (!criteria.hasValidLength) feedback.append("Password should be at least 8 characters. ");
        if (!criteria.hasUppercase) feedback.append("Add uppercase letters. ");
        if (!criteria.hasLowercase) feedback.append("Add lowercase letters. ");
        if (!criteria.hasDigit) feedback.append("Add numbers. ");
        if (!criteria.hasSpecial) feedback.append("Add special characters (@$!%*?&#). ");

        return feedback.toString().trim();
    }

    private String determinePasswordLevel(int score) {
        if (score >= 80) return "STRONG";
        if (score >= 60) return "MEDIUM";
        if (score >= 40) return "WEAK";
        return "VERY_WEAK";
    }

    private PasswordStrengthResult createWeakResult(String message) {
        return new PasswordStrengthResult(0, "VERY_WEAK", message);
    }

    /**
     * Mask phone number for display
     */
    public String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return phoneNumber;
        }

        PhoneNumberParts parts = extractPhoneNumberParts(phoneNumber);
        return parts.countryCode + "*".repeat(parts.middleLength) + parts.lastFour;
    }

    private PhoneNumberParts extractPhoneNumberParts(String phoneNumber) {
        String countryCode = phoneNumber.substring(0, Math.min(4, phoneNumber.length()));
        String lastFour = phoneNumber.substring(Math.max(phoneNumber.length() - 4, 4));
        int middleLength = Math.max(phoneNumber.length() - countryCode.length() - lastFour.length(), 0);

        return new PhoneNumberParts(countryCode, lastFour, middleLength);
    }

    /**
     * Mask email for display
     */
    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) {
            return email; // Don't mask very short usernames
        }

        String maskedUsername = createMaskedUsername(username);
        return maskedUsername + "@" + domain;
    }

    private String createMaskedUsername(String username) {
        char firstChar = username.charAt(0);
        char lastChar = username.charAt(username.length() - 1);
        String middleMask = "*".repeat(username.length() - 2);
        return firstChar + middleMask + lastChar;
    }

    // Helper classes for better organization
    private static class PasswordCriteria {
        final boolean hasValidLength;
        final boolean hasUppercase;
        final boolean hasLowercase;
        final boolean hasDigit;
        final boolean hasSpecial;

        PasswordCriteria(boolean hasValidLength, boolean hasUppercase, boolean hasLowercase,
                         boolean hasDigit, boolean hasSpecial) {
            this.hasValidLength = hasValidLength;
            this.hasUppercase = hasUppercase;
            this.hasLowercase = hasLowercase;
            this.hasDigit = hasDigit;
            this.hasSpecial = hasSpecial;
        }
    }

    private static class PhoneNumberParts {
        final String countryCode;
        final String lastFour;
        final int middleLength;

        PhoneNumberParts(String countryCode, String lastFour, int middleLength) {
            this.countryCode = countryCode;
            this.lastFour = lastFour;
            this.middleLength = middleLength;
        }
    }

    /**
     * Password strength analysis result
     */
    @Getter
    public static class PasswordStrengthResult {
        private final int score;
        private final String level;
        private final String message;

        public PasswordStrengthResult(int score, String level, String message) {
            this.score = score;
            this.level = level;
            this.message = message;
        }

    }
}