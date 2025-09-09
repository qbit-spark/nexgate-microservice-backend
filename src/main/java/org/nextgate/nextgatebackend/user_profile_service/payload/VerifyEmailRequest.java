package org.nextgate.nextgatebackend.user_profile_service.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyEmailRequest {

    @NotBlank(message = "Temp token is required")
    private String tempToken;

    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "\\d{6}", message = "OTP code must be a 6-digit number")
    private String otpCode;
}