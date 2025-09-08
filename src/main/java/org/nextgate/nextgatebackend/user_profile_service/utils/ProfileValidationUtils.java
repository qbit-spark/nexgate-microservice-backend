package org.nextgate.nextgatebackend.user_profile_service.utils;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ProfileValidationUtils {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]*$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$");

    private static final List<String> RESERVED_USERNAMES = Arrays.asList(
            "admin", "administrator", "root", "system", "api", "www", "mail",
            "support", "help", "info", "contact", "service", "user", "guest",
            "null", "undefined", "test", "demo", "example", "nexgate",
            "buildwise", "official", "staff", "moderator", "bot", "operator",
            "manager", "owner", "public", "private", "security", "abuse"
    );

    /**
     * Validate username format and rules
     */
    public boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        String trimmedUsername = username.trim();
        return trimmedUsername.length() >= 3 &&
                trimmedUsername.length() <= 30 &&
                USERNAME_PATTERN.matcher(trimmedUsername).matches();
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
        if (password == null || password.length() < 8) {
            return false;
        }
        return STRONG_PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Sanitize input by trimming whitespace
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return input.trim();
    }

    /**
     * Check password strength and return detailed analysis
     */
    public PasswordStrengthResult analyzePasswordStrength(String password) {
        if (password == null) {
            return new PasswordStrengthResult(0, "VERY_WEAK", "Password is required");
        }

        int score = 0;
        StringBuilder feedback = new StringBuilder();

        // Length check
        if (password.length() >= 8) {
            score += 20;
        } else {
            feedback.append("Password should be at least 8 characters. ");
        }

        // Uppercase check
        if (password.matches(".*[A-Z].*")) {
            score += 20;
        } else {
            feedback.append("Add uppercase letters. ");
        }

        // Lowercase check
        if (password.matches(".*[a-z].*")) {
            score += 20;
        } else {
            feedback.append("Add lowercase letters. ");
        }

        // Digit check
        if (password.matches(".*\\d.*")) {
            score += 20;
        } else {
            feedback.append("Add numbers. ");
        }

        // Special character check
        if (password.matches(".*[@$!%*?&#].*")) {
            score += 20;
        } else {
            feedback.append("Add special characters (@$!%*?&#). ");
        }

        String level;
        if (score >= 80) level = "STRONG";
        else if (score >= 60) level = "MEDIUM";
        else if (score >= 40) level = "WEAK";
        else level = "VERY_WEAK";

        String message = feedback.length() > 0 ? feedback.toString().trim() : "Password meets all requirements";

        return new PasswordStrengthResult(score, level, message);
    }

    /**
     * Mask phone number for display
     */
    public String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return phoneNumber;
        }

        String countryCode = phoneNumber.substring(0, Math.min(4, phoneNumber.length()));
        String lastFour = phoneNumber.substring(Math.max(phoneNumber.length() - 4, 4));
        int starsCount = phoneNumber.length() - countryCode.length() - lastFour.length();
        String stars = "*".repeat(Math.max(starsCount, 0));

        return countryCode + stars + lastFour;
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

        String maskedUsername = username.charAt(0) + "*".repeat(username.length() - 2) + username.charAt(username.length() - 1);
        return maskedUsername + "@" + domain;
    }

    /**
     * Password strength analysis result
     */
    public static class PasswordStrengthResult {
        private final int score;
        private final String level;
        private final String message;

        public PasswordStrengthResult(int score, String level, String message) {
            this.score = score;
            this.level = level;
            this.message = message;
        }

        public int getScore() { return score; }
        public String getLevel() { return level; }
        public String getMessage() { return message; }
    }
}
