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
public class PhoneVerificationResponse {
    private String tempToken;
    private String message;
    private LocalDateTime expireAt;
    private String phoneNumber; // Masked for security (e.g., +255*****1234)

    public PhoneVerificationResponse(String tempToken, String message, LocalDateTime expireAt) {
        this.tempToken = tempToken;
        this.message = message;
        this.expireAt = expireAt;
    }

    // Utility method to mask phone number
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return phoneNumber;
        }

        String countryCode = phoneNumber.substring(0, 4); // +255
        String lastFour = phoneNumber.substring(phoneNumber.length() - 4);
        String stars = "*".repeat(phoneNumber.length() - 8);

        return countryCode + stars + lastFour;
    }
}
