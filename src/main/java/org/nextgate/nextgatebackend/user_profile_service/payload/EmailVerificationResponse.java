package org.nextgate.nextgatebackend.user_profile_service.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationResponse {
    private String tempToken;
    private String message;
    private LocalDateTime expireAt;
    private String email; // Masked for security (e.g., jo***@example.com)

    public EmailVerificationResponse(String tempToken, String message, LocalDateTime expireAt) {
        this.tempToken = tempToken;
        this.message = message;
        this.expireAt = expireAt;
    }

    // Utility method to mask email
    public static String maskEmail(String email) {
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
}